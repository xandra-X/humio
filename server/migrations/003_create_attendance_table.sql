CREATE TABLE IF NOT EXISTS Attendance (
  attendance_id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  date DATE NOT NULL,
  check_in_time DATETIME NULL,
  check_out_time DATETIME NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_att_user FOREIGN KEY (user_id) REFERENCES `User` (user_id)
);

CREATE INDEX IF NOT EXISTS idx_att_user_date ON Attendance(user_id, date);
