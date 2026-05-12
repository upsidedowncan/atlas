use std::collections::HashMap;
use std::net::SocketAddr;
use std::sync::Arc;

use base64::{engine::general_purpose::STANDARD as BASE64, Engine};
use futures_util::{SinkExt, StreamExt};
use log::{error, info};
use rand::rngs::OsRng;
use rand::RngCore;
use rusqlite::{params, Connection};
use serde::{Deserialize, Serialize};
use sha2::{Digest, Sha256};
use tokio::net::TcpStream;
use tokio::sync::Mutex;
use tokio_tungstenite::{accept_async, tungstenite::Message};

type SharedState = Arc<Mutex<State>>;

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(tag = "type")]
enum ClientFrame {
    #[serde(rename = "auth_register")]
    AuthRegister {
        username: String,
        password: String,
        #[serde(rename = "publicKey")]
        public_key: String,
    },
    #[serde(rename = "auth_login")]
    AuthLogin {
        username: String,
        password: String,
        #[serde(rename = "publicKey")]
        public_key: String,
    },
    #[serde(rename = "message")]
    Message {
        id: String,
        to: String,
        payload: EncryptedPayload,
        #[serde(rename = "senderPayload")]
        sender_payload: EncryptedPayload,
        #[serde(rename = "timestampMs")]
        timestamp_ms: i64,
    },
    #[serde(rename = "fetch_key")]
    FetchKey { username: String },
    #[serde(rename = "search_users")]
    SearchUsers { query: String },
    #[serde(rename = "update_public_status")]
    UpdatePublicStatus { 
        #[serde(rename = "isPublic")]
        is_public: bool 
    },
    #[serde(rename = "fetch_public_users")]
    FetchPublicUsers,
    #[serde(rename = "delete_conversation")]
    DeleteConversation { peer: String },
    #[serde(rename = "get_unread")]
    GetUnread,
    #[serde(rename = "clear_unread")]
    ClearUnread { peer: String },
    #[serde(rename = "update_avatar")]
    UpdateAvatar { data: String },
    #[serde(rename = "fetch_avatar")]
    FetchAvatar { username: String },
    #[serde(rename = "edit_message")]
    EditMessage { id: String, payload: EncryptedPayload, sender_payload: EncryptedPayload },
    #[serde(rename = "delete_message")]
    DeleteMessage { id: String },
    #[serde(rename = "atlas_broadcast_dialog")]
    AtlasBroadcastDialog { id: String, text: String, #[serde(rename = "imageUrl")] image_url: Option<String>, #[serde(rename = "timestampMs")] timestamp_ms: i64 },
}

#[derive(Debug, Clone, Serialize, Deserialize)]
struct EncryptedPayload {
    #[serde(rename = "encryptedKey")]
    encrypted_key: String,
    iv: String,
    ciphertext: String,
    tag: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(tag = "type")]
enum ServerFrame {
    #[serde(rename = "auth_ok")]
    AuthOk { 
        username: String,
        #[serde(rename = "isPublic")]
        is_public: bool
    },
    #[serde(rename = "error")]
    ServerError { message: String },
    #[serde(rename = "public_key")]
    PublicKeyReceived { 
        username: String, 
        #[serde(rename = "publicKey")]
        public_key: String 
    },
    #[serde(rename = "key_not_found")]
    KeyNotFound { username: String },
    #[serde(rename = "message")]
    MessageReceived {
        id: String,
        from: String,
        to: String,
        payload: EncryptedPayload,
        #[serde(rename = "timestampMs")]
        timestamp_ms: i64,
    },
    #[serde(rename = "message_history")]
    MessageHistory { messages: Vec<HistoryEntry> },
    #[serde(rename = "user_joined")]
    UserJoined { username: String },
    #[serde(rename = "user_left")]
    UserLeft { username: String },
    #[serde(rename = "user_list")]
    UserList { users: Vec<String> },
    #[serde(rename = "search_results")]
    SearchResults { users: Vec<String> },
    #[serde(rename = "public_users")]
    PublicUsers { users: Vec<PublicUserInfo> },
    #[serde(rename = "conversation_deleted")]
    ConversationDeleted { peer: String },
    #[serde(rename = "unread_counts")]
    UnreadCounts { counts: HashMap<String, i32> },
    #[serde(rename = "unread_cleared")]
    UnreadCleared { peer: String },
    #[serde(rename = "avatar_response")]
    AvatarResponse { username: String, data: Option<String> },
    #[serde(rename = "message_edited")]
    MessageEdited { id: String, from: String, to: String, payload: EncryptedPayload, sender_payload: EncryptedPayload },
    #[serde(rename = "message_deleted")]
    MessageDeleted { id: String },
    #[serde(rename = "atlas_dialog")]
    AtlasDialog { id: String, text: String, #[serde(rename = "imageUrl")] image_url: Option<String>, #[serde(rename = "timestampMs")] timestamp_ms: i64 },
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PublicUserInfo {
    pub username: String,
    #[serde(rename = "isOnline")]
    pub is_online: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
struct HistoryEntry {
    id: String,
    from: String,
    to: String,
    payload: EncryptedPayload,
    #[serde(rename = "timestampMs")]
    timestamp_ms: i64,
}

struct OnlineUser {
    username: String,
    public_key: String,
    tx: tokio::sync::mpsc::UnboundedSender<Message>,
}

struct State {
    online: HashMap<String, OnlineUser>,
    db: Connection,
}

impl State {
    fn new(db: Connection) -> Self {
        Self {
            online: HashMap::new(),
            db,
        }
    }
}

fn hash_password(password: &str, _salt: &[u8]) -> String {
    let mut salt_bytes = [0u8; 12];
    OsRng.fill_bytes(&mut salt_bytes);
    
    let mut hasher = Sha256::new();
    hasher.update(&salt_bytes);
    hasher.update(password.as_bytes());
    let hash_result = hasher.finalize();
    
    format!("{}:{}", BASE64.encode(salt_bytes), BASE64.encode(hash_result))
}

fn verify_password(password: &str, stored: &str) -> bool {
    let parts: Vec<&str> = stored.split(':').collect();
    if parts.len() != 2 { return false; }
    
    let salt = match BASE64.decode(parts[0]) {
        Ok(s) => s,
        Err(_) => return false,
    };
    let stored_hash = match BASE64.decode(parts[1]) {
        Ok(h) => h,
        Err(_) => return false,
    };
    
    let mut hasher = Sha256::new();
    hasher.update(&salt);
    hasher.update(password.as_bytes());
    let computed = hasher.finalize();
    
    computed.as_slice() == stored_hash.as_slice()
}

async fn handle_connection(ws: tokio_tungstenite::WebSocketStream<TcpStream>, addr: SocketAddr, state: SharedState) {
    info!("New connection from: {}", addr);

    let (mut ws_sender, mut ws_receiver) = ws.split();
    let (tx, mut rx) = tokio::sync::mpsc::unbounded_channel::<Message>();

    let mut username: Option<String> = None;

    loop {
        tokio::select! {
            msg = ws_receiver.next() => {
                match msg {
                    Some(Ok(Message::Text(text))) => {
                        if let Err(e) = handle_message(&text, &mut username, &state, &tx).await {
                            error!("Error handling message: {}", e);
                            let error_msg = ServerFrame::ServerError { message: e.to_string() };
                            if let Ok(json) = serde_json::to_string(&error_msg) {
                                let _ = tx.send(Message::Text(json.into()));
                            }
                        }
                    }
                    Some(Ok(Message::Close(_))) | None => break,
                    Some(Err(e)) => {
                        error!("WebSocket error: {}", e);
                        break;
                    }
                    _ => {}
                }
            }
            Some(out_msg) = rx.recv() => {
                if let Err(e) = ws_sender.send(out_msg).await {
                    error!("Error sending to WS: {}", e);
                    break;
                }
            }
        }
    }

    if let Some(user) = username.take() {
        let mut s = state.lock().await;
        s.online.remove(&user);
        info!("User '{}' disconnected ({} online)", user, s.online.len());
    }
}

async fn handle_message(
    text: &str,
    username: &mut Option<String>,
    state: &SharedState,
    tx: &tokio::sync::mpsc::UnboundedSender<Message>,
) -> Result<(), Box<dyn std::error::Error + Send + Sync>> {
    let frame: ClientFrame = serde_json::from_str(text)?;
    let mut s = state.lock().await;

    match frame {
        ClientFrame::AuthRegister { username: uname, password, public_key } => {
            if uname.len() < 2 || uname.len() > 32 {
                return Err("Username must be 2-32 characters".into());
            }
            if password.len() < 4 {
                return Err("Password must be at least 4 characters".into());
            }

            let exists: i32 = s.db.query_row(
                "SELECT COUNT(*) FROM users WHERE username = ?",
                [&uname],
                |row| row.get(0),
            )?;
            if exists > 0 {
                return Err(format!("User '{}' already exists", uname).into());
            }

            let hash = hash_password(&password, &[]);

            s.db.execute(
                "INSERT INTO users (username, password_hash, public_key) VALUES (?, ?, ?)",
                params![uname.clone(), hash, public_key.clone()],
            )?;

            *username = Some(uname.clone());
            s.online.insert(uname.clone(), OnlineUser { 
                username: uname.clone(), 
                public_key,
                tx: tx.clone() 
            });

            info!("Registered: {} ({} online)", uname, s.online.len());
            send_frame(tx, ServerFrame::AuthOk { 
                username: uname.clone(),
                is_public: false 
            })?;
            send_frame(tx, ServerFrame::UserList { users: s.online.keys().cloned().collect() })?;
            
            let history = fetch_history(&s.db, &uname)?;
            send_frame(tx, ServerFrame::MessageHistory { messages: history })?;
            send_atlas_dialog_history(&s.db, tx)?;

            Ok(())
        }

        ClientFrame::AuthLogin { username: uname, password, public_key } => {
            let stored_hash: String = match s.db.query_row(
                "SELECT password_hash FROM users WHERE username = ?",
                [&uname],
                |row| row.get(0),
            ) {
                Ok(h) => h,
                Err(_) => return Err("User not found".into()),
            };

            if !verify_password(&password, &stored_hash) {
                return Err("Invalid password".into());
            }

            let is_public: i32 = s.db.query_row(
                "SELECT is_public FROM users WHERE username = ?",
                [&uname],
                |row| row.get(0),
            )?;

            s.db.execute(
                "UPDATE users SET public_key = ? WHERE username = ?",
                params![public_key.clone(), &uname],
            )?;

            *username = Some(uname.clone());
            s.online.insert(uname.clone(), OnlineUser { 
                username: uname.clone(), 
                public_key,
                tx: tx.clone()
            });

            info!("Login: {} ({} online)", uname, s.online.len());
            send_frame(tx, ServerFrame::AuthOk { 
                username: uname.clone(),
                is_public: is_public == 1
            })?;
            send_frame(tx, ServerFrame::UserList { users: s.online.keys().cloned().collect() })?;

            let history = fetch_history(&s.db, &uname)?;
            send_frame(tx, ServerFrame::MessageHistory { messages: history })?;
            send_atlas_dialog_history(&s.db, tx)?;

            Ok(())
        }

        ClientFrame::FetchKey { username: target } => {
            if let Some(user) = s.online.get(&target) {
                send_frame(tx, ServerFrame::PublicKeyReceived {
                    username: target,
                    public_key: user.public_key.clone(),
                })?;
            } else if let Ok(stored_key) = s.db.query_row::<String, _, _>(
                "SELECT public_key FROM users WHERE username = ?",
                [&target],
                |row| row.get(0),
            ) {
                send_frame(tx, ServerFrame::PublicKeyReceived {
                    username: target,
                    public_key: stored_key,
                })?;
            } else {
                send_frame(tx, ServerFrame::KeyNotFound { username: target })?;
            }
            Ok(())
        }

        ClientFrame::SearchUsers { query } => {
            let sender = username.as_ref().ok_or("Not authenticated")?;
            let search = format!("%{}%", query.to_lowercase());
            let mut stmt = s.db.prepare(
                "SELECT username FROM users WHERE username != ? AND username != 'atlas' AND LOWER(username) LIKE ? LIMIT 10"
            )?;
            let mut rows = stmt.query(params![sender, search])?;
            let mut results = Vec::new();
            while let Some(row) = rows.next()? {
                if let Ok(u) = row.get::<_, String>(0) {
                    results.push(u);
                }
            }
            send_frame(tx, ServerFrame::SearchResults { users: results })?;
            Ok(())
        }

        ClientFrame::UpdatePublicStatus { is_public } => {
            let uname = username.as_ref().ok_or("Not authenticated")?;
            s.db.execute(
                "UPDATE users SET is_public = ? WHERE username = ?",
                params![if is_public { 1 } else { 0 }, uname],
            )?;
            Ok(())
        }

        ClientFrame::FetchPublicUsers => {
            let uname = username.as_ref().ok_or("Not authenticated")?;
            let mut stmt = s.db.prepare(
                "SELECT username FROM users WHERE is_public = 1 AND username != ?"
            )?;
            let mut rows = stmt.query(params![uname])?;
            let mut public_users = Vec::new();
            while let Some(row) = rows.next()? {
                let name: String = row.get(0)?;
                let is_online = s.online.contains_key(&name);
                public_users.push(PublicUserInfo {
                    username: name,
                    is_online,
                });
            }
            send_frame(tx, ServerFrame::PublicUsers { users: public_users })?;
            Ok(())
        }

            ClientFrame::Message { id, to, payload, sender_payload, timestamp_ms } => {
            let sender = username.as_ref().ok_or("Not authenticated")?;
            if to == "atlas" {
                return Err("Cannot message 'atlas' user".into());
            }

            s.db.execute(
                "INSERT OR IGNORE INTO messages (id, from_user, to_user, encrypted_key, iv, ciphertext, tag, sender_encrypted_key, sender_iv, sender_ciphertext, sender_tag, timestamp_ms)
                 VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                params![
                    id, sender, to,
                    payload.encrypted_key, payload.iv, payload.ciphertext, payload.tag,
                    sender_payload.encrypted_key, sender_payload.iv, sender_payload.ciphertext, sender_payload.tag,
                    timestamp_ms,
                ],
            )?;

            if let Some(recipient) = s.online.get(&to) {
                let msg = ServerFrame::MessageReceived {
                    id: id.clone(),
                    from: sender.clone(),
                    to: to.clone(),
                    payload: payload.clone(),
                    timestamp_ms,
                };
                let _ = recipient.tx.send(Message::Text(serde_json::to_string(&msg)?.into()));
            } else {
                s.db.execute(
                    "INSERT OR IGNORE INTO unread (id, from_user, to_user, timestamp_ms) VALUES (?, ?, ?, ?)",
                    params![id, sender, to, timestamp_ms],
                )?;
            }

            let msg_self = ServerFrame::MessageReceived {
                id,
                from: sender.clone(),
                to,
                payload: sender_payload,
                timestamp_ms,
            };
            send_frame(tx, msg_self)?;

            Ok(())
        }

        ClientFrame::DeleteConversation { peer } => {
            let sender = username.as_ref().ok_or("Not authenticated")?;
            s.db.execute(
                "DELETE FROM messages WHERE (from_user = ? AND to_user = ?) OR (from_user = ? AND to_user = ?)",
                params![sender, peer, peer, sender],
            )?;
            s.db.execute(
                "DELETE FROM unread WHERE (to_user = ? AND from_user = ?) OR (to_user = ? AND from_user = ?)",
                params![sender, peer, peer, sender],
            )?;
            send_frame(tx, ServerFrame::ConversationDeleted { peer })?;
            Ok(())
        }

        ClientFrame::GetUnread => {
            let sender = username.as_ref().ok_or("Not authenticated")?;
            let mut stmt = s.db.prepare("SELECT from_user FROM unread WHERE to_user = ?")?;
            let mut rows = stmt.query(params![sender])?;
            let mut counts: HashMap<String, i32> = HashMap::new();
            while let Some(row) = rows.next()? {
                let from: String = row.get(0)?;
                *counts.entry(from).or_insert(0) += 1;
            }
            send_frame(tx, ServerFrame::UnreadCounts { counts })?;
            Ok(())
        }

        ClientFrame::ClearUnread { peer } => {
            let sender = username.as_ref().ok_or("Not authenticated")?;
            s.db.execute(
                "DELETE FROM unread WHERE to_user = ? AND from_user = ?",
                params![sender, peer],
            )?;
            send_frame(tx, ServerFrame::UnreadCleared { peer })?;
            Ok(())
        }

        ClientFrame::UpdateAvatar { data } => {
            let sender = username.as_ref().ok_or("Not authenticated")?;
            s.db.execute(
                "UPDATE users SET avatar_url = ? WHERE username = ?",
                params![data, sender],
            )?;
            send_frame(tx, ServerFrame::AvatarResponse {
                username: sender.clone(),
                data: Some(data),
            })?;
            Ok(())
        }

        ClientFrame::FetchAvatar { username: target } => {
            let _sender = username.as_ref().ok_or("Not authenticated")?;
            let avatar_url: Option<String> = s.db.query_row(
                "SELECT avatar_url FROM users WHERE username = ?",
                [&target],
                |row| row.get(0),
            ).ok();
            send_frame(tx, ServerFrame::AvatarResponse {
                username: target,
                data: avatar_url,
            })?;
            Ok(())
        }

        ClientFrame::EditMessage { id, payload, sender_payload } => {
            let sender = username.as_ref().ok_or("Not authenticated")?;
            
            s.db.execute(
                "UPDATE messages SET encrypted_key = ?, iv = ?, ciphertext = ?, tag = ?, sender_encrypted_key = ?, sender_iv = ?, sender_ciphertext = ?, sender_tag = ? WHERE id = ? AND from_user = ?",
                params![
                    payload.encrypted_key, payload.iv, payload.ciphertext, payload.tag,
                    sender_payload.encrypted_key, sender_payload.iv, sender_payload.ciphertext, sender_payload.tag,
                    id, sender,
                ],
            )?;

            s.db.query_row::<(String, String), _, _>(
                "SELECT from_user, to_user FROM messages WHERE id = ?",
                [&id],
                |row| Ok((row.get(0)?, row.get(1)?)),
            ).ok().map(|(from, to)| {
                let _ = send_frame(tx, ServerFrame::MessageEdited {
                    id: id.clone(),
                    from: from.clone(),
                    to: to.clone(),
                    payload: payload.clone(),
                    sender_payload: sender_payload.clone(),
                });
                if let Some(recipient) = s.online.get(&if from == *sender { to.clone() } else { from.clone() }) {
                    let _ = recipient.tx.send(Message::Text(serde_json::to_string(&ServerFrame::MessageEdited {
                        id: id.clone(),
                        from: from.clone(),
                        to: if from == *sender { to.clone() } else { to },
                        payload: payload.clone(),
                        sender_payload: sender_payload.clone(),
                    }).unwrap().into()));
                }
            });

            Ok(())
        }

        ClientFrame::DeleteMessage { id } => {
            let sender = username.as_ref().ok_or("Not authenticated")?;
            
            s.db.execute(
                "DELETE FROM messages WHERE id = ? AND from_user = ?",
                params![id, sender],
            )?;

            let _ = send_frame(tx, ServerFrame::MessageDeleted { id: id.clone() });
            
            Ok(())
        }
        ClientFrame::AtlasBroadcastDialog { id, text, image_url, timestamp_ms } => {
            let sender = username.as_ref().ok_or("Not authenticated")?;
            if sender != "atlas" {
                return Err("Only 'atlas' can broadcast dialogs".into());
            }
            s.db.execute(
                "INSERT OR IGNORE INTO atlas_dialogs (id, text, image_url, timestamp_ms) VALUES (?, ?, ?, ?)",
                params![id, text, image_url, timestamp_ms],
            )?;
            let frame = ServerFrame::AtlasDialog { id, text, image_url, timestamp_ms };
            let raw = serde_json::to_string(&frame)?;
            for (_, user) in s.online.iter() {
                let _ = user.tx.send(Message::Text(raw.clone().into()));
            }
            Ok(())
        }
    }
}

fn send_frame(tx: &tokio::sync::mpsc::UnboundedSender<Message>, frame: ServerFrame) -> Result<(), Box<dyn std::error::Error + Send + Sync>> {
    let json = serde_json::to_string(&frame)?;
    tx.send(Message::Text(json.into()))?;
    Ok(())
}

fn send_atlas_dialog_history(
    db: &Connection,
    tx: &tokio::sync::mpsc::UnboundedSender<Message>,
) -> Result<(), Box<dyn std::error::Error + Send + Sync>> {
    let mut stmt = db.prepare("SELECT id, text, image_url, timestamp_ms FROM atlas_dialogs ORDER BY timestamp_ms ASC")?;
    let mut rows = stmt.query([])?;
    while let Some(row) = rows.next()? {
        send_frame(tx, ServerFrame::AtlasDialog {
            id: row.get(0)?,
            text: row.get(1)?,
            image_url: row.get(2)?,
            timestamp_ms: row.get(3)?,
        })?;
    }
    Ok(())
}

fn fetch_history(db: &Connection, username: &str) -> Result<Vec<HistoryEntry>, rusqlite::Error> {
    let mut stmt = db.prepare(
        "SELECT id, from_user, to_user, 
                encrypted_key, iv, ciphertext, tag,
                sender_encrypted_key, sender_iv, sender_ciphertext, sender_tag,
                timestamp_ms 
         FROM messages 
         WHERE from_user = ? OR to_user = ?
         ORDER BY timestamp_ms ASC"
    )?;
    
    let rows = stmt.query_map(params![username, username], |row| {
        let from_user: String = row.get(1)?;
        let is_sender = from_user == username;
        
        let (ek, iv, ct, tag) = if is_sender {
            (row.get::<_, String>(7)?, row.get::<_, String>(8)?, row.get::<_, String>(9)?, row.get::<_, String>(10)?)
        } else {
            (row.get::<_, String>(3)?, row.get::<_, String>(4)?, row.get::<_, String>(5)?, row.get::<_, String>(6)?)
        };
        
        Ok(HistoryEntry {
            id: row.get(0)?,
            from: from_user,
            to: row.get(2)?,
            payload: EncryptedPayload {
                encrypted_key: ek,
                iv,
                ciphertext: ct,
                tag,
            },
            timestamp_ms: row.get(11)?,
        })
    })?;
    
    let mut messages = Vec::new();
    for row in rows {
        messages.push(row?);
    }
    Ok(messages)
}

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    env_logger::init();

    let db = Connection::open("atlas.db")?;
    
    // Migration: Add is_public column if it doesn't exist
    let _ = db.execute("ALTER TABLE users ADD COLUMN avatar_url TEXT DEFAULT NULL", []);

    db.execute_batch(
        "CREATE TABLE IF NOT EXISTS users (
            username TEXT PRIMARY KEY,
            password_hash TEXT NOT NULL,
            public_key TEXT NOT NULL,
            is_public INTEGER DEFAULT 0,
            avatar_url TEXT DEFAULT NULL
        );
        CREATE TABLE IF NOT EXISTS messages (
            id TEXT PRIMARY KEY,
            from_user TEXT NOT NULL,
            to_user TEXT NOT NULL,
            encrypted_key TEXT NOT NULL,
            iv TEXT NOT NULL,
            ciphertext TEXT NOT NULL,
            tag TEXT NOT NULL,
            sender_encrypted_key TEXT NOT NULL,
            sender_iv TEXT NOT NULL,
            sender_ciphertext TEXT NOT NULL,
            sender_tag TEXT NOT NULL,
            timestamp_ms INTEGER NOT NULL
        );
        CREATE TABLE IF NOT EXISTS unread (
            id TEXT PRIMARY KEY,
            from_user TEXT NOT NULL,
            to_user TEXT NOT NULL,
            timestamp_ms INTEGER NOT NULL
        );
        CREATE INDEX IF NOT EXISTS idx_messages_to_user ON messages(to_user);
        CREATE INDEX IF NOT EXISTS idx_messages_from_user ON messages(from_user);
        CREATE INDEX IF NOT EXISTS idx_unread_to_user ON unread(to_user);
        CREATE TABLE IF NOT EXISTS atlas_dialogs (
            id TEXT PRIMARY KEY,
            text TEXT NOT NULL,
            image_url TEXT,
            timestamp_ms INTEGER NOT NULL
        );"
    )?;

    let state = Arc::new(Mutex::new(State::new(db)));

    let listener = tokio::net::TcpListener::bind("0.0.0.0:8080").await?;
    info!("Atlas server listening on 0.0.0.0:8080");

    while let Ok((stream, addr)) = listener.accept().await {
        let state = state.clone();
        tokio::spawn(async move {
            if let Ok(ws) = accept_async(stream).await {
                handle_connection(ws, addr, state).await;
            }
        });
    }

    Ok(())
}
