require("dotenv").config();
const express = require("express");
const bodyParser = require("body-parser");
const cors = require("cors");
const mysql = require("mysql2");

const app = express();
const PORT = process.env.PORT || 3001;
const admin = require("firebase-admin");
const serviceAccount = require("D:/Tugas Rezky/taskly-backend/tasklyapk-firebase-adminsdk-g24iy-c2e24fcb72.json");
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
});

// Middleware
app.use(bodyParser.json());
app.use(cors());

// Database connection
const db = mysql.createConnection({
  host: process.env.DB_HOST,
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME,
});

// Helper function for sending FCM notifications
const sendNotification = (token, title, body, channelId) => {
  const message = {
    notification: {
      title,
      body,
    },
    data: {
      channelId,
    },
    token,
  };

  return admin.messaging().send(message).catch((err) => {
    console.error("Gagal mengirim notifikasi:", err.message);
  });
};


db.connect((err) => {
  if (err) {
    console.error("Database connection failed:", err.message);
  } else {
    console.log("Connected to the database.");
  }
});

// Routes
app.get("/", (req, res) => {
  db.ping((err) => {
    if (err) {
      res.status(500).json({ message: "Database not connected", error: err.message });
    } else {
      res.status(200).json({ message: "Database connected successfully" });
    }
  });
});

// User routes
app.post("/register", (req, res) => {
  const { username, email, password } = req.body;
  const query = "INSERT INTO users (username, email, password) VALUES (?, ?, ?)";
  db.query(query, [username, email, password], (err) => {
    if (err) {
      res.status(500).json({ message: "Registration failed", error: err });
    } else {
      res.status(201).json({ message: "User registered successfully" });
    }
  });
});

app.post("/login", (req, res) => {
  const { email, password } = req.body;
  const query = "SELECT * FROM users WHERE email = ? AND password = ?";
  db.query(query, [email, password], (err, result) => {
    if (err) {
      res.status(500).json({ success: false, message: "Login failed", error: err });
    } else if (result.length > 0) {
      const user = result[0];
      res.status(200).json({
        success: true,
        message: "Login successful",
        user: {
          id: user.id,
          username: user.username,
          email: user.email,
        },
      });
    } else {
      res.status(401).json({ success: false, message: "Invalid email or password" });
    }
  });
});

app.get("/users", (req, res) => {
  const query = "SELECT id, username, email FROM users";
  db.query(query, (err, results) => {
      if (err) {
          res.status(500).json({ message: "Error fetching users", error: err });
      } else {
          res.status(200).json(results);
      }
  });
});

app.get("/users/:userId", (req, res) => {
  const { userId } = req.params;

  const query = "SELECT id, username, email FROM users WHERE id = ?";
  db.query(query, [userId], (err, results) => {
    if (err) {
      res.status(500).json({ message: "Error fetching user details", error: err });
    } else if (results.length === 0) {
      res.status(404).json({ message: "User not found" });
    } else {
      res.status(200).json(results[0]);
    }
  });
});

// FCM 
app.post('/updateToken', (req, res) => {
  const { userId, token } = req.body;
  console.log(`Received token for user ${userId}: ${token}`); // Log token
  if (!userId || !token) {
    return res.status(400).json({ message: "User ID and token are required" });
  }

  const query = "UPDATE users SET fcm_token = ? WHERE id = ?";
  db.query(query, [token, userId], (err) => {
    if (err) {
      console.error("Error updating FCM token:", err);
      res.status(500).json({ message: "Failed to update token", error: err.message });
    } else {
      res.status(200).json({ message: "Token updated successfully" });
    }
  });
});


// **Personal Tasks**
app.post('/personal_tasks', (req, res) => {
  const { nama_tugas, tanggal, deadline, user_id } = req.body;

  const query = `
    INSERT INTO personal_tasks (nama_tugas, tanggal, deadline, user_id, is_completed)
    VALUES (?, ?, ?, ?, false)
  `;

  db.query(query, [nama_tugas, tanggal, deadline, user_id], (err, result) => {
    if (err) {
      res.status(500).json({ message: "Gagal menambahkan tugas.", error: err.message });
    } else {
      res.status(201).json({ message: result.insertId.toString() });

      // Fetch FCM token for the user
      const tokenQuery = "SELECT fcm_token FROM users WHERE id = ?";
      db.query(tokenQuery, [user_id], (tokenErr, tokenResults) => {
        if (tokenErr || tokenResults.length === 0) {
          console.error("Gagal mendapatkan token FCM:", tokenErr?.message || "User tidak ditemukan.");
          return;
        }

        const fcmToken = tokenResults[0].fcm_token;
        const title = "Tugas Baru Ditambahkan";
        const body = `Tugas "${nama_tugas}" telah ditambahkan. Deadline: ${deadline}.`;

        // Kirim notifikasi
        sendNotification(fcmToken, title, body, "personaltask_added")
          .then(() => {
            console.log("Tugas berhasil ditambahkan dan notifikasi dikirim.");
          })
          .catch((notifErr) => {
            console.error("Gagal mengirim notifikasi:", notifErr.message);
          });
      });
    }
  });
});

app.get('/personal_tasks/:userId', (req, res) => {
  const { userId } = req.params;

  // Ambil semua tugas pribadi user
  const query = `
      SELECT id, nama_tugas, tanggal, deadline, is_completed
      FROM personal_tasks
      WHERE user_id = ?
  `;

  db.query(query, [userId], (err, tasks) => {
    if (err) {
      res.status(500).json({ message: "Error fetching tasks.", error: err });
    } else if (tasks.length === 0) {
      res.status(200).json([]); // Tidak ada tugas
    } else {
      const taskIds = tasks.map(task => task.id);
      const subtasksQuery = `
        SELECT * FROM personal_subtasks WHERE tugas_pribadi_id IN (?)
      `;

      // Ambil semua subtasks terkait
      db.query(subtasksQuery, [taskIds], (subtaskErr, subtasks) => {
        if (subtaskErr) {
          res.status(500).json({ message: "Error fetching subtasks.", error: subtaskErr });
        } else {
            // Sanitasi `is_completed` di subtasks
            const sanitizedSubtasks = subtasks.map((subtask) => ({
              ...subtask,
              is_completed: subtask.is_completed === 1,
            }));
          // Gabungkan subtasks ke masing-masing tugas
          const tasksWithSubtasks = tasks.map(task => ({
            ...task,
            subtasks: subtasks.filter(subtask => subtask.tugas_pribadi_id === task.id),
            is_completed: !!task.is_completed // Konversi ke boolean
          }));

          res.status(200).json(tasksWithSubtasks);
        }
      });
    }
  });
});


app.get('/personal_tasks/in_progress/:userId', (req, res) => {
  const { userId } = req.params;
  const query = `
    SELECT pt.id, pt.nama_tugas, pt.tanggal, pt.deadline, pt.user_id, pt.is_completed
    FROM personal_tasks pt
    LEFT JOIN personal_subtasks ps ON pt.id = ps.tugas_pribadi_id
    WHERE pt.user_id = ? AND pt.is_completed = false
    GROUP BY pt.id
    HAVING SUM(CASE WHEN ps.is_completed = false THEN 1 ELSE 0 END) > 0 OR COUNT(ps.id) = 0
  `;
  db.query(query, [userId], (err, results) => {
    if (err) {
      console.error("Error fetching in-progress tasks:", err);
      res.status(500).json({ message: "Error fetching in-progress tasks.", error: err.message });
    } else {
      const sanitizedResults = results.map((task) => ({
        ...task,
        is_completed: !!task.is_completed,
      }));
      res.status(200).json(sanitizedResults);
    }
  });
});

app.get("/personal_tasks/completed/:userId", (req, res) => {
  const { userId } = req.params;

  const query = `
    SELECT pt.id, pt.nama_tugas, pt.tanggal, pt.deadline 
    FROM personal_tasks pt
    WHERE pt.user_id = ? AND pt.is_completed = true
  `;

  db.query(query, [userId], (err, results) => {
    if (err) {
      console.error("Error fetching completed personal tasks:", err);
      res.status(500).json({ message: "Error fetching completed personal tasks.", error: err.message });
    } else {
      res.status(200).json(results);
    }
  });
});

// Update task completion status
app.put("/personal_tasks/:id", (req, res) => {
  const { id } = req.params;
  const { is_completed } = req.body;

  const updateQuery = "UPDATE personal_tasks SET is_completed = ? WHERE id = ?";
  db.query(updateQuery, [is_completed, id], (err) => {
    if (err) {
      console.error("Error updating task status:", err);
      res.status(500).json({ message: "Error updating task.", error: err.message });
    } else {
      res.status(200).json({ message: "Task updated successfully." });
    }
  });
});

// **Personal Subtasks**
app.get('/personal_subtasks/:taskId', (req, res) => {
  const { taskId } = req.params;
  const query = `
    SELECT id, nama_sub_tugas, tugas_pribadi_id, is_completed
    FROM personal_subtasks
    WHERE tugas_pribadi_id = ?
  `;
  db.query(query, [taskId], (err, results) => {
    if (err) {
      console.error("Error fetching subtasks:", err);
      res.status(500).json({ message: "Error fetching subtasks.", error: err.message });
    } else {
      const sanitizedResults = results.map((subtask) => ({
        ...subtask,
        is_completed: !!subtask.is_completed,
      }));
      res.status(200).json(sanitizedResults);
    }
  });
});

app.post('/personal_subtasks', (req, res) => {
  const { nama_sub_tugas, tugas_pribadi_id, is_completed } = req.body;
  const query = `
      INSERT INTO personal_subtasks (nama_sub_tugas, tugas_pribadi_id, is_completed)
      VALUES (?, ?, ?)
  `;
  db.query(query, [nama_sub_tugas, tugas_pribadi_id, is_completed], (err) => {
      if (err) throw err;
      res.json({ message: 'Personal Subtask added successfully' }); 
  });
});

app.put("/personal_subtasks/:id", (req, res) => {
  const { id } = req.params;
  const { is_completed } = req.body;

  if (typeof is_completed !== "boolean") {
    return res.status(400).json({ message: "is_completed harus berupa boolean." });
  }

  if (!id) {
    return res.status(400).json({ message: "ID Subtask diperlukan." });
  }

  const updateSubtaskQuery = "UPDATE personal_subtasks SET is_completed = ? WHERE id = ?";
  db.query(updateSubtaskQuery, [is_completed, id], (err, results) => {
    if (err) {
      console.error("Error updating subtask:", err);
      return res.status(500).json({ message: "Error updating subtask.", error: err.message });
    }

    const checkParentTaskQuery = `SELECT tugas_pribadi_id,
             SUM(CASE WHEN is_completed = false THEN 1 ELSE 0 END) AS unfinished
      FROM personal_subtasks
      WHERE tugas_pribadi_id = (SELECT tugas_pribadi_id FROM personal_subtasks WHERE id = ?)
      GROUP BY tugas_pribadi_id`;

    db.query(checkParentTaskQuery, [id], (checkErr, results) => {
      if (checkErr) {
        console.error("Error checking parent task:", checkErr);
        return res.status(500).json({ message: "Error checking parent task.", error: checkErr.message });
      }

      const { tugas_pribadi_id, unfinished } = results[0];
      if (unfinished === 0) {
        const completeParentTaskQuery = "UPDATE personal_tasks SET is_completed = true WHERE id = ?";
        db.query(completeParentTaskQuery, [tugas_pribadi_id], (completeErr) => {
          if (completeErr) {
            console.error("Error completing parent task:", completeErr);
            return res.status(500).json({ message: "Error completing parent task.", error: completeErr.message });
          }
          res.status(200).json({ message: "Subtask and parent task updated successfully." });
        });
      } else {
        res.status(200).json({ message: "Subtask updated successfully." });
      }
    });
  });
});

// **Group Tasks**
app.post('/group_tasks', (req, res) => {
  const { nama_tugas, tanggal, deadline, anggota } = req.body;

  // Query untuk menambahkan tugas kelompok
  const query = `
      INSERT INTO group_tasks (nama_tugas, tanggal, deadline, is_completed)
      VALUES (?, ?, ?, false)
  `;

  db.query(query, [nama_tugas, tanggal, deadline], (err, results) => {
    if (err) {
      console.error("Error inserting group task:", err);
      res.status(500).json({
        message: "Gagal menambahkan tugas kelompok.",
        error: err.message
      });
    } else {
      const taskId = results.insertId; // ID tugas yang baru dimasukkan
      console.log("Inserted Task ID:", taskId);

      // Jika ada anggota yang ditambahkan
      if (anggota && anggota.length > 0) {
        const memberQuery = `
          INSERT INTO group_task_members (tugas_kelompok_id, user_id)
          VALUES ?
        `;
        const values = anggota.map((userId) => [taskId, userId]);

        db.query(memberQuery, [values], (memberErr) => {
          if (memberErr) {
            console.error("Error inserting group members:", memberErr);
            res.status(500).json({
              message: "Gagal menambahkan anggota kelompok.",
              error: memberErr.message
            });
          } else {
            res.status(201).json({
              message: taskId,
              id: taskId,
              nama_tugas,
              tanggal,
              deadline,
              is_completed: false // Explicitly set as boolean
            });

            // Ambil token berdasarkan user_id anggota
            const tokenQuery = `
              SELECT fcm_token 
              FROM users 
              WHERE id IN (?)
            `;
            db.query(tokenQuery, [anggota], (tokenErr, tokenResults) => {
              if (tokenErr || tokenResults.length === 0) {
                console.error("Gagal mendapatkan token FCM anggota kelompok:", tokenErr?.message || "Tidak ada anggota.");
              return;
            }

            const tokens = tokenResults[0].fcm_token;
            const title = "Tugas Kelompok Baru Ditambahkan";
            const body = `Tugas "${nama_tugas}" telah ditambahkan. Deadline: ${deadline}.`;

            tokens.forEach(token => {
              sendNotification(token, title, body, "grouptask_added")
                .then(() => {
                  console.log("Tugas berhasil ditambahkan dan notifikasi dikirim.");
                })
                .catch((notifErr) => {
                  console.error("Gagal mengirim notifikasi:", notifErr.message);
                });
            });
          });
      }});
      } else {
        // Jika tidak ada anggota yang ditambahkan
        res.status(201).json({
          message: taskId,
          id: taskId,
          nama_tugas,
          tanggal,
          deadline,
          is_completed: false // Explicitly set as boolean
        });
      }
    }
  });
});

app.get('/group_tasks/:userId', (req, res) => {
  const { userId } = req.params;

  if (!userId) {
    return res.status(400).json({ message: "UserId is required." });
  }

  const query = `
      SELECT gt.id, gt.nama_tugas, gt.tanggal, gt.deadline, gt.is_completed 
      FROM group_tasks gt 
      JOIN group_task_members gtm ON gt.id = gtm.tugas_kelompok_id 
      WHERE gtm.user_id = ?
  `;

  db.query(query, [userId], (err, tasks) => {
    if (err) {
      console.error("Error fetching group tasks:", err);
      return res.status(500).json({ message: "Gagal mengambil tugas kelompok.", error: err.message });
    }

    if (tasks.length === 0) {
      return res.status(200).json({ inProgress: [], completed: [] }); // Tidak ada tugas kelompok
    }

    const sanitizedTasks = tasks.map(task => ({
      ...task,
      is_completed: !!task.is_completed, // Konversi ke boolean
    }));

    const taskIds = sanitizedTasks.map(task => task.id);
    const subtasksQuery = `
        SELECT tugas_kelompok_id, id, nama_sub_tugas, is_completed 
        FROM group_subtasks 
        WHERE tugas_kelompok_id IN (?)
    `;

    db.query(subtasksQuery, [taskIds], (subtaskErr, subtasks) => {
      if (subtaskErr) {
        console.error("Error fetching subtasks:", subtaskErr);
        return res.status(500).json({ message: "Gagal mengambil subtasks.", error: subtaskErr.message });
      }

      const sanitizedSubtasks = subtasks.map(subtask => ({
        ...subtask,
        is_completed: !!subtask.is_completed, // Konversi ke boolean
      }));

      // Filter tugas berdasarkan status subtasks
      const inProgressTasks = sanitizedTasks.filter(task => {
        const relatedSubtasks = sanitizedSubtasks.filter(subtask => subtask.tugas_kelompok_id === task.id);
        return relatedSubtasks.some(subtask => !subtask.is_completed); // Ada subtasks belum selesai
      });

      const completedTasks = sanitizedTasks.filter(task => {
        const relatedSubtasks = sanitizedSubtasks.filter(subtask => subtask.tugas_kelompok_id === task.id);
        return relatedSubtasks.every(subtask => subtask.is_completed); // Semua subtasks selesai
      });

      // Ambil anggota untuk setiap tugas
      const membersQuery = `
          SELECT tugas_kelompok_id, users.username 
          FROM group_task_members 
          JOIN users ON group_task_members.user_id = users.id 
          WHERE tugas_kelompok_id IN (?)
      `;

      db.query(membersQuery, [taskIds], (memberErr, members) => {
        if (memberErr) {
          console.error("Error fetching task members:", memberErr);
          return res.status(500).json({ message: "Gagal mengambil anggota tugas.", error: memberErr.message });
        }

        const inProgressDetails = inProgressTasks.map(task => ({
          ...task,
          subtasks: sanitizedSubtasks.filter(subtask => subtask.tugas_kelompok_id === task.id),
          members: members
            .filter(member => member.tugas_kelompok_id === task.id)
            .map(member => member.username),
        }));

        const completedDetails = completedTasks.map(task => ({
          ...task,
          subtasks: sanitizedSubtasks.filter(subtask => subtask.tugas_kelompok_id === task.id),
          members: members
            .filter(member => member.tugas_kelompok_id === task.id)
            .map(member => member.username),
        }));

        res.status(200).json({ inProgress: inProgressDetails, completed: completedDetails });
      });
    });
  });
});

// In Progress Tasks
app.get("/group_tasks/in_progress/:userId", (req, res) => {
  const { userId } = req.params;
  const query = `
    SELECT gt.id, gt.nama_tugas, gt.tanggal, gt.deadline 
    FROM group_tasks gt
    JOIN group_task_members gtm ON gt.id = gtm.tugas_kelompok_id
    WHERE gtm.user_id = ? AND gt.is_completed = false
  `;
  db.query(query, [userId], (err, results) => {
    if (err) {
      res.status(500).json({ message: "Error fetching in-progress tasks", error: err.message });
    } else {
      res.status(200).json(results);
    }
  });
});

// Completed Tasks
app.get("/group_tasks/completed/:userId", (req, res) => {
  const { userId } = req.params;
  const query = `
    SELECT gt.id, gt.nama_tugas, gt.tanggal, gt.deadline 
    FROM group_tasks gt
    JOIN group_task_members gtm ON gt.id = gtm.tugas_kelompok_id
    WHERE gtm.user_id = ? AND gt.is_completed = true
  `;
  db.query(query, [userId], (err, results) => {
    if (err) {
      res.status(500).json({ message: "Error fetching completed tasks", error: err.message });
    } else {
      res.status(200).json(results);
    }
  });
});

app.put('/group_tasks/:id', (req, res) => {
  const { id } = req.params;
  const { is_completed } = req.body; // Ambil `is_completed` dari objek `GroupTask`

  if (typeof is_completed !== 'boolean') {
      return res.status(400).json({ message: "is_completed harus berupa boolean." });
  }

  const query = 'UPDATE group_tasks SET is_completed = ? WHERE id = ?';
  db.query(query, [is_completed, id], (err) => {
      if (err) {
          console.error("Error updating group task:", err);
          return res.status(500).json({ message: "Gagal memperbarui tugas kelompok.", error: err.message });
      }
      res.status(200).json({ message: "Group task updated successfully" });
  });
});

// **Group Subtasks**
app.get('/group_subtasks/:taskId', (req, res) => {
  const { taskId } = req.params;
  const query = 'SELECT id, nama_sub_tugas, tugas_kelompok_id, is_completed FROM group_subtasks WHERE tugas_kelompok_id = ?';

  db.query(query, [taskId], (err, results) => {
    if (err) {
      console.error("Error fetching subtasks:", err);
      res.status(500).json({ message: "Error fetching subtasks.", error: err.message });
    } else {
      // Konversi is_completed ke boolean
      const sanitizedResults = results.map(subtask => ({
        ...subtask,
        is_completed: !!subtask.is_completed, // Konversi 0/1 ke false/true
      }));
      res.status(200).json(sanitizedResults);
    }
  });
});

app.post('/group_subtasks', (req, res) => {
  const { nama_sub_tugas, tugas_kelompok_id } = req.body;
  const query = `
      INSERT INTO group_subtasks (nama_sub_tugas, tugas_kelompok_id, is_completed)
      VALUES (?, ?, false)
  `;
  db.query(query, [nama_sub_tugas, tugas_kelompok_id], (err) => {
      if (err) {
          console.error("Error inserting subtask:", err);
          res.status(500).json({ message: 'Gagal menambahkan sub-tugas.', error: err.message });
      } else {
          res.status(201).json({ message: 'Subtask berhasil ditambahkan.' });
      }
  });
});

app.put("/group_subtasks/:id", (req, res) => {
  const { id } = req.params;
  const { is_completed } = req.body;

  const updateQuery = "UPDATE group_subtasks SET is_completed = ? WHERE id = ?";
  db.query(updateQuery, [is_completed, id], (err) => {
    if (err) return res.status(500).json({ message: "Error updating subtask.", error: err.message });

    const checkQuery = `
      SELECT tugas_kelompok_id,
             SUM(CASE WHEN is_completed = false THEN 1 ELSE 0 END) AS unfinished
      FROM group_subtasks
      WHERE tugas_kelompok_id = (SELECT tugas_kelompok_id FROM group_subtasks WHERE id = ?)
    `;
    db.query(checkQuery, [id], (checkErr, results) => {
      if (checkErr) return res.status(500).json({ message: "Error checking task status.", error: checkErr.message });

      const { tugas_kelompok_id, unfinished } = results[0];
      if (unfinished === 0) {
        const completeTaskQuery = "UPDATE group_tasks SET is_completed = true WHERE id = ?";
        db.query(completeTaskQuery, [tugas_kelompok_id], (completeErr) => {
          if (completeErr) {
            return res.status(500).json({ message: "Error completing task.", error: completeErr.message });
          }
          res.status(200).json({ message: "Subtask and task completed successfully." });
        });
      } else {
        res.status(200).json({ message: "Subtask updated successfully." });
      }
    });
  });
});

// **Group Task Members**
app.get('/group_task_members/:taskId', (req, res) => {
  const { taskId } = req.params;

  // Log taskId untuk debugging
  console.log(`Fetching members for taskId: ${taskId}`);

  const query = `
      SELECT users.id AS user_id, users.username, users.email 
      FROM group_task_members 
      JOIN users ON group_task_members.user_id = users.id 
      WHERE group_task_members.tugas_kelompok_id = ?
  `;

  db.query(query, [taskId], (err, results) => {
    if (err) {
      console.error("Error fetching members:", err);

      // Kirim respons dengan error
      return res.status(500).json({
        message: "Gagal mengambil anggota tugas.",
        error: err.message
      });
    }

    if (results.length === 0) {
      console.log(`No members found for taskId: ${taskId}`);

      // Kirim respons jika tidak ada anggota
      return res.status(404).json({
        message: `Tidak ada anggota yang ditemukan untuk tugas dengan ID ${taskId}.`
      });
    }

    // Log hasil query untuk debugging
    console.log(`Members fetched for taskId ${taskId}:`, results);

    // Kirim respons sukses
    res.status(200).json(results);
  });
});


app.put('/group_task_members/:taskId', (req, res) => {
  console.log("Updating Group Members:", req.body);
  const { members } = req.body;
  const { taskId } = req.params;
  if (!Array.isArray(members)) {
    return res.status(400).json({ message: "Members harus berupa array." });
  }
  const deleteQuery = 'DELETE FROM group_task_members WHERE tugas_kelompok_id = ?';
  db.query(deleteQuery, [taskId], (err) => {
    if (err) {
      console.error(err);
      res.status(500).json({ message: "Gagal menghapus anggota sebelumnya." });
    } else if (members.length > 0) {
      const insertQuery = 'INSERT INTO group_task_members (tugas_kelompok_id, user_id) VALUES ?';
      const values = members.map((userId) => [taskId, userId]);
      db.query(insertQuery, [values], (err) => {
        if (err) {
          console.error(err);
          res.status(500).json({ message: "Gagal menambahkan anggota baru." });
        } else {
          res.json({ message: "Anggota kelompok berhasil diperbarui." });
        }
      });
    } else {
      res.json({ message: "Semua anggota dihapus." });
    }
  });
});

const cron = require("node-cron");

cron.schedule("0 * * * *", () => {
  const query = `
    SELECT pt.id, pt.nama_tugas, pt.deadline, u.fcm_token 
    FROM personal_tasks pt 
    JOIN users u ON pt.user_id = u.id 
    WHERE pt.is_completed = false AND TIMESTAMPDIFF(HOUR, NOW(), pt.deadline) <= 24
  `;

  db.query(query, (err, results) => {
    if (err) {
      console.error("Error fetching tasks near deadline:", err.message);
      return;
    }

    results.forEach(task => {
      const title = "Pengingat Deadline Tugas Pribadi";
      const body = `Tugas "${task.nama_tugas}" akan segera jatuh tempo pada ${task.deadline}.`;
      sendNotification(task.fcm_token, title, body, "deadline_personal")
        .catch(notifErr => console.error("Gagal mengirim notifikasi deadline:", notifErr.message));
    });
  });
});

cron.schedule("0 * * * *", () => {
  const query = `
    SELECT gt.id, gt.nama_tugas, gt.deadline, u.fcm_token 
    FROM group_tasks gt 
    JOIN group_task_members gtm ON gt.id = gtm.tugas_kelompok_id 
    JOIN users u ON gtm.user_id = u.id 
    WHERE gt.is_completed = false AND TIMESTAMPDIFF(HOUR, NOW(), gt.deadline) <= 24
  `;

  db.query(query, (err, results) => {
    if (err) {
      console.error("Error fetching group tasks near deadline:", err.message);
      return;
    }

    results.forEach(task => {
      const title = "Pengingat Deadline Tugas Kelompok";
      const body = `Tugas "${task.nama_tugas}" akan segera jatuh tempo pada ${task.deadline}.`;
      sendNotification(task.fcm_token, title, body, "deadline_group")
        .catch(notifErr => console.error("Gagal mengirim notifikasi deadline kelompok:", notifErr.message));
    });
  });
});

app.listen(PORT, () => {
  console.log(`Server running on http://localhost:${PORT}`);
});
