import sqlite3
import os

db_path = r'C:\Users\user\myagent.db'
conn = sqlite3.connect(db_path)
cur = conn.cursor()
cur.execute("SELECT uuid, type, uri FROM media WHERE type='video' ORDER BY created_at DESC LIMIT 10")
rows = cur.fetchall()
print(f"Всего видео в БД: {len(rows)}")
for i, r in enumerate(rows):
    print(f"{i+1}. UUID: {r[0]}, type: {r[1]}, uri: {r[2]}")
conn.close()