import { Database } from "bun:sqlite";

const PORT = Number(process.env.PORT ?? 8080);
const db = new Database("atlas.db", { create: true });

db.run(`
  CREATE TABLE IF NOT EXISTS users (
    username     TEXT PRIMARY KEY,
    passwordHash TEXT NOT NULL,
    publicKey    TEXT NOT NULL
  )
`);

db.run(`
  CREATE TABLE IF NOT EXISTS messages (
    id                   TEXT    PRIMARY KEY,
    fromUser             TEXT    NOT NULL,
    toUser               TEXT    NOT NULL,
    encryptedKey         TEXT    NOT NULL,
    iv                   TEXT    NOT NULL,
    ciphertext           TEXT    NOT NULL,
    tag                  TEXT    NOT NULL,
    senderEncryptedKey   TEXT    NOT NULL,
    senderIv             TEXT    NOT NULL,
    senderCiphertext     TEXT    NOT NULL,
    senderTag            TEXT    NOT NULL,
    timestampMs          INTEGER NOT NULL
  )
`);

db.run(`CREATE INDEX IF NOT EXISTS idx_messages_toUser   ON messages(toUser)`);
db.run(`CREATE INDEX IF NOT EXISTS idx_messages_fromUser ON messages(fromUser)`);
db.run(`CREATE INDEX IF NOT EXISTS idx_unread_toUser      ON unread(toUser)`);

interface OnlineEntry {
  socket: ServerWebSocket<{ username: string }>;
  publicKey: string;
}

interface EncryptedPayload {
  encryptedKey: string;
  iv: string;
  ciphertext: string;
  tag: string;
}

type ClientFrame =
  | { type: "auth_register"; username: string; password: string; publicKey: string }
  | { type: "auth_login"; username: string; password: string; publicKey: string }
  | {
      type: "message";
      id: string;
      to: string;
      payload: EncryptedPayload;
      senderPayload: EncryptedPayload;
      timestampMs: number;
    }
  | { type: "fetch_key"; username: string }
  | { type: "search_users"; query: string };

const onlineUsers = new Map<string, OnlineEntry>();

const stmtInsertUser = db.prepare(
  "INSERT INTO users (username, passwordHash, publicKey) VALUES (?, ?, ?)"
);
const stmtUpdatePublicKey = db.prepare(
  "UPDATE users SET publicKey = ? WHERE username = ?"
);
const stmtGetUser = db.prepare<{ passwordHash: string; publicKey: string }, [string]>(
  "SELECT passwordHash, publicKey FROM users WHERE username = ?"
);
const stmtUserExists = db.prepare<{ count: number }, [string]>(
  "SELECT COUNT(*) as count FROM users WHERE username = ?"
);
const stmtInsertMessage = db.prepare(`
  INSERT OR IGNORE INTO messages
    (id, fromUser, toUser,
     encryptedKey, iv, ciphertext, tag,
     senderEncryptedKey, senderIv, senderCiphertext, senderTag,
     timestampMs)
  VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
`);
const stmtGetHistoryForUser = db.prepare<{
  id: string; fromUser: string; toUser: string;
  encryptedKey: string; iv: string; ciphertext: string; tag: string;
  senderEncryptedKey: string; senderIv: string; senderCiphertext: string; senderTag: string;
  timestampMs: number;
}, [string, string]>(
  "SELECT * FROM messages WHERE toUser = ? OR fromUser = ? ORDER BY timestampMs ASC"
);
const stmtSearchUsers = db.prepare<{ username: string }, [string, string]>(
  "SELECT username FROM users WHERE username != ? AND username LIKE ?"
);
const stmtGetAvatar = db.prepare<{ avatarUrl: string }, [string]>(
  "SELECT avatarUrl FROM users WHERE username = ?"
);
const stmtUpdateAvatar = db.prepare("UPDATE users SET avatarUrl = ? WHERE username = ?");
const stmtDeleteConversation = db.prepare(
  "DELETE FROM messages WHERE (fromUser = ? AND toUser = ?) OR (fromUser = ? AND toUser = ?)"
);
const stmtGetUnread = db.prepare<{ fromUser: string; id: string; timestamp: number }, [string]>(
  "SELECT fromUser, id, timestamp FROM unread WHERE toUser = ?"
);
const stmtInsertUnread = db.prepare(
  "INSERT OR IGNORE INTO unread (id, fromUser, toUser, timestamp) VALUES (?, ?, ?, ?)"
);
const stmtDeleteUnreadForPeer = db.prepare("DELETE FROM unread WHERE toUser = ? AND fromUser = ?");

function log(message: string): void {
  console.log(`[${new Date().toISOString()}] ${message}`);
}

function send(socket: ServerWebSocket<unknown>, data: unknown): void {
  socket.send(JSON.stringify(data));
}

function broadcastPresence(
  type: "user_joined" | "user_left",
  username: string,
  exclude?: ServerWebSocket<unknown>,
): void {
  for (const [, entry] of onlineUsers) {
    if (entry.socket !== exclude) {
      send(entry.socket, { type, username });
    }
  }
}

function sendHistory(socket: ServerWebSocket<unknown>, username: string): void {
  const rows = stmtGetHistoryForUser.all(username, username);
  if (rows.length === 0) return;
  send(socket, {
    type: "message_history",
    messages: rows.map((r) => {
      const isSender = r.fromUser === username;
      const payload = isSender
        ? { encryptedKey: r.senderEncryptedKey, iv: r.senderIv, ciphertext: r.senderCiphertext, tag: r.senderTag }
        : { encryptedKey: r.encryptedKey, iv: r.iv, ciphertext: r.ciphertext, tag: r.tag };
      return { id: r.id, from: r.fromUser, to: r.toUser, payload, timestampMs: r.timestampMs };
    }),
  });
}

function broadcastUnreadCount(username: string): void {
  const unreadRows = stmtGetUnread.all(username);
  const unreadMap: Record<string, number> = {};
  for (const row of unreadRows) {
    unreadMap[row.fromUser] = (unreadMap[row.fromUser] || 0) + 1;
  }
  sendToUser(username, { type: "unread_counts", counts: unreadMap });
}

function sendToUser(username: string, data: unknown): void {
  const entry = onlineUsers.get(username);
  if (entry) send(entry.socket, data);
}

Bun.serve<{ username: string }>({
  port: PORT,
  hostname: "0.0.0.0",

  fetch(req, server) {
    const upgraded = server.upgrade(req, { data: { username: "" } });
    if (upgraded) return;
    return new Response("Сервер Atlas: WebSocket-эндпоинт.", {
      status: 426,
      headers: { "Content-Type": "text/plain; charset=utf-8" },
    });
  },

  websocket: {
    open(socket) {
      log("Новое подключение (ожидание аутентификации).");
    },

    async message(socket, rawMessage) {
      let frame: ClientFrame;
      try {
        frame = JSON.parse(rawMessage as string) as ClientFrame;
      } catch {
        send(socket, { type: "error", message: "Невалидный JSON." });
        return;
      }

      switch (frame.type) {
        case "auth_register": {
          const { username, password, publicKey } = frame;
          if (!username || username.length < 2 || username.length > 32) {
            send(socket, { type: "error", message: "Имя пользователя должно быть от 2 до 32 символов." });
            return;
          }
          if (!password || password.length < 4) {
            send(socket, { type: "error", message: "Пароль должен быть минимум 4 символа." });
            return;
          }
          const exists = stmtUserExists.get(username);
          if (exists && exists.count > 0) {
            send(socket, { type: "error", message: `Пользователь «${username}» уже зарегистрирован.` });
            return;
          }
          if (onlineUsers.has(username)) {
            send(socket, { type: "error", message: `Пользователь «${username}» уже в сети.` });
            return;
          }
          const passwordHash = await Bun.password.hash(password);
          stmtInsertUser.run(username, passwordHash, publicKey);
          socket.data.username = username;
          onlineUsers.set(username, { socket, publicKey });
          log(`Регистрация: ${username} (всего онлайн: ${onlineUsers.size})`);
          send(socket, { type: "auth_ok", username });
          send(socket, { type: "user_list", users: [...onlineUsers.keys()] });
          sendHistory(socket, username);
          broadcastPresence("user_joined", username, socket);
          break;
        }

        case "auth_login": {
          const { username, password, publicKey } = frame;
          const record = stmtGetUser.get(username);
          if (!record) {
            send(socket, { type: "error", message: `Пользователь «${username}» не зарегистрирован.` });
            return;
          }
          if (onlineUsers.has(username)) {
            send(socket, { type: "error", message: `Пользователь «${username}» уже в сети.` });
            return;
          }
          const valid = await Bun.password.verify(password, record.passwordHash);
          if (!valid) {
            send(socket, { type: "error", message: "Неверный пароль." });
            return;
          }
          stmtUpdatePublicKey.run(publicKey, username);
          socket.data.username = username;
          onlineUsers.set(username, { socket, publicKey });
          log(`Вход: ${username} (всего онлайн: ${onlineUsers.size})`);
          send(socket, { type: "auth_ok", username });
          send(socket, { type: "user_list", users: [...onlineUsers.keys()] });
          sendHistory(socket, username);
          broadcastPresence("user_joined", username, socket);
          break;
        }

        case "fetch_key": {
          const { username } = frame;
          const entry = onlineUsers.get(username);
          if (entry) {
            send(socket, { type: "public_key", username, publicKey: entry.publicKey });
            return;
          }
          const record = stmtGetUser.get(username);
          if (!record) {
            send(socket, { type: "key_not_found", username });
            return;
          }
          send(socket, { type: "public_key", username, publicKey: record.publicKey });
          break;
        }

        case "search_users": {
          const sender = socket.data.username;
          if (!sender) {
            send(socket, { type: "error", message: "Необходимо сначала войти." });
            return;
          }
          const query = `%${frame.query.toLowerCase().trim()}%`;
          const results = stmtSearchUsers.all(sender, query).map((r) => r.username);
          send(socket, { type: "search_results", users: results });
          break;
        }

        case "delete_conversation": {
          const sender = socket.data.username;
          if (!sender) {
            send(socket, { type: "error", message: "Необходимо сначала войти." });
            return;
          }
          stmtDeleteConversation.run(sender, frame.peer, frame.peer, sender);
          stmtDeleteUnreadForPeer.run(sender, frame.peer);
          send(socket, { type: "conversation_deleted", peer: frame.peer });
          break;
        }

        case "get_unread": {
          const sender = socket.data.username;
          if (!sender) {
            send(socket, { type: "error", message: "Необходимо сначала войти." });
            return;
          }
          broadcastUnreadCount(sender);
          break;
        }

        case "clear_unread": {
          const sender = socket.data.username;
          if (!sender) {
            send(socket, { type: "error", message: "Необходимо сначала войти." });
            return;
          }
          stmtDeleteUnreadForPeer.run(sender, frame.peer);
          broadcastUnreadCount(sender);
          break;
        }

        case "update_avatar": {
          const sender = socket.data.username;
          if (!sender) {
            send(socket, { type: "error", message: "Необходимо сначала войти." });
            return;
          }
          stmtUpdateAvatar.run(frame.avatarUrl, sender);
          send(socket, { type: "avatar_updated", avatarUrl: frame.avatarUrl });
          break;
        }

        case "fetch_avatar": {
          const sender = socket.data.username;
          if (!sender) {
            send(socket, { type: "error", message: "Необходимо сначала войти." });
            return;
          }
          const row = stmtGetAvatar.get(frame.username);
          const avatarUrl = row?.avatarUrl ?? null;
          send(socket, { type: "avatar_response", username: frame.username, avatarUrl });
          break;
        }

        case "message": {
          const sender = socket.data.username;
          if (!sender) {
            send(socket, { type: "error", message: "Необходимо сначала войти." });
            return;
          }
          const { id, to, payload, senderPayload, timestampMs } = frame;
          stmtInsertMessage.run(
            id, sender, to,
            payload.encryptedKey, payload.iv, payload.ciphertext, payload.tag,
            senderPayload.encryptedKey, senderPayload.iv, senderPayload.ciphertext, senderPayload.tag,
            timestampMs,
          );
          const recipient = onlineUsers.get(to);
          if (recipient) {
            send(recipient.socket, { type: "message", id, from: sender, to, payload, timestampMs });
          } else {
            stmtInsertUnread.run(id, sender, to, timestampMs);
            sendToUser(sender, { type: "unread_recorded", peer: to });
          }
          send(socket, { type: "message", id, from: sender, to, payload: senderPayload, timestampMs });
          log(`Зашифрованное сообщение: ${sender} → ${to}`);
          break;
        }

        default: {
          send(socket, { type: "error", message: "Неизвестный тип команды." });
        }
      }
    },

    close(socket) {
      const username = socket.data.username;
      if (!username) return;
      onlineUsers.delete(username);
      log(`Отключение: ${username} (осталось онлайн: ${onlineUsers.size})`);
      broadcastPresence("user_left", username);
    },

    error(socket, error) {
      log(`Ошибка сокета [${socket.data.username ?? "неизвестный"}]: ${error.message}`);
    },
  },
});

log(`Сервер Atlas запущен на порту ${PORT}.`);
