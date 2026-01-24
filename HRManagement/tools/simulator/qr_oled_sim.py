
import _tkinter
import os
import time
import argparse
import threading
import json
from datetime import datetime
from PIL import Image, ImageTk
import qrcode
import tkinter as tk
import requests

def current_window(interval_seconds: int) -> int:
    return int(time.time()) // interval_seconds

def make_payload(device_id: str, window: int) -> str:
    return f"{device_id}|{window}"

def make_qr_image(payload: str, size_px: int = 256) -> Image.Image:
    qr = qrcode.QRCode(
        version=None,
        error_correction=qrcode.constants.ERROR_CORRECT_M,
        box_size=10,
        border=2
    )
    qr.add_data(payload)
    qr.make(fit=True)
    img = qr.make_image(fill_color="black", back_color="white").convert("RGB")
    img = img.resize((size_px, size_px), Image.NEAREST)
    return img

def post_scan(server_url: str, auth_token: str, x_user_id: str, action: str, payload: str, timeout: int = 6):
    url = server_url.rstrip("/") + "/api/dashboard/check"  # same endpoint used by your app (DashboardApi)
    headers = {
        "Authorization": f"Bearer {auth_token}" if auth_token else "",
    }
    if x_user_id:
        headers["X-User-Id"] = str(x_user_id)

    body = {
        "action": action,
        "qr": payload
    }

    try:
        resp = requests.post(url, json=body, headers=headers, timeout=timeout)
        try:
            j = resp.json()
        except Exception:
            j = {"status_text": resp.text}
        return resp.status_code, j
    except Exception as e:
        return None, {"error": str(e)}

class QrSimulator:
    def __init__(self, device_id: str, interval: int = 30, out_dir: str = "out",
                 server_url: str = None, auth_token: str = None, x_user_id: str = None,
                 auto_post: bool = False, action: str = "CHECK_IN"):
        self.device_id = device_id
        self.interval = int(interval)
        self.server_url = server_url
        self.auth_token = auth_token
        self.x_user_id = x_user_id
        self.auto_post = bool(auto_post)
        self.action = action
        self.out_dir = out_dir
        os.makedirs(self.out_dir, exist_ok=True)

        self._stop = threading.Event()
        self._lock = threading.Lock()
        self.payload = None
        self.last_window = None
        self.last_image_path = None

        self.root = tk.Tk()
        self.root.title(f"QR OLED Simulator — {self.device_id}")
        self.root.geometry("320x360")
        self.root.resizable(False, False)

        self.canvas = tk.Canvas(self.root, width=280, height=280, bg="black", highlightthickness=0)
        self.canvas.pack(pady=8)
        self.status_label = tk.Label(self.root, text="Initializing...", font=("Helvetica", 10))
        self.status_label.pack()

        self.root.bind("<space>", lambda e: self.simulate_scan())
        self.root.protocol("WM_DELETE_WINDOW", self.stop)  # handle window close

    def regenerate_now(self):
        with self._lock:
            w = current_window(self.interval)
            self.last_window = w
            self.payload = make_payload(self.device_id, w)
            img = make_qr_image(self.payload, size_px=280)
            timestamp = datetime.utcnow().strftime("%Y%m%dT%H%M%SZ")
            fname = f"{self.device_id}_{w}_{timestamp}.png"
            path = os.path.join(self.out_dir, fname)
            img.save(path)
            self.last_image_path = path
            self._show_image(img)
            self.status_label.config(text=f"Payload: {self.payload}   (saved: {os.path.basename(path)})")

    def _show_image(self, pil_img):
        
        self.tk_img = ImageTk.PhotoImage(pil_img)
        self.canvas.delete("all")
        self.canvas.create_image(140, 140, image=self.tk_img)

    def start_background_loop(self):
        def loop():
            while not self._stop.is_set():
                try:
                    self.regenerate_now()
                    if self.auto_post:
                        code, resp = post_scan(
                            server_url=self.server_url, auth_token=self.auth_token,
                            x_user_id=self.x_user_id, action=self.action, payload=self.payload
                        )
                        print(f"[AUTO POST] status={code} resp={resp}")
                    now = time.time()
                    next_window_start = ((int(now) // self.interval) + 1) * self.interval
                    wait = max(1, next_window_start - now)
                    for i in range(int(wait)):
                        if self._stop.is_set():
                            break
                        time.sleep(1)
                except Exception as e:
                    print("Loop error:", e)
                    time.sleep(1)
        t = threading.Thread(target=loop, daemon=True)
        t.start()

    def simulate_scan(self):
        payload = self.payload
        if not payload:
            print("No payload to scan")
            return
        print(f"[SIMULATED SCAN] payload={payload}")
        if self.server_url and self.auth_token:
            code, resp = post_scan(
                server_url=self.server_url, auth_token=self.auth_token,
                x_user_id=self.x_user_id, action=self.action, payload=payload
            )
            print("[SIMULATED SCAN] POST result:", code, resp)
            self.status_label.config(text=f"Last scan POST: {code} | {resp.get('message') if isinstance(resp, dict) else resp}")
        else:
            self.status_label.config(text=f"Simulated scan: {payload} (not posted)")

    def run(self):
        self.start_background_loop()
        self.root.mainloop()

    def stop(self):
        self._stop.set()
        try:
            self.root.destroy()
        except:
            pass

def parse_args():
    p = argparse.ArgumentParser(description="QR OLED Simulator")
    p.add_argument("--device", "-d", default="sim-device-1", help="Device ID string used in payload")
    p.add_argument("--interval", "-i", type=int, default=30, help="Window size in seconds (default 30)")
    p.add_argument("--out", default="out", help="Output folder for saved QR images")
    p.add_argument("--server", help="Server base URL (e.g. http://10.0.2.2:8080 or http://192.168.1.142:8080)")
    p.add_argument("--auth", help="Bearer token (JWT) to use for POST")
    p.add_argument("--x-user-id", help="Optional X-User-Id header numeric value")
    p.add_argument("--auto-post", action="store_true", help="Automatically POST to server on generation")
    p.add_argument("--action", default="CHECK_IN", choices=["CHECK_IN", "CHECK_OUT"], help="Action used when POSTing")
    return p.parse_args()

def main():
    args = parse_args()
    sim = QrSimulator(
        device_id=args.device,
        interval=args.interval,
        out_dir=args.out,
        server_url=args.server,
        auth_token=args.auth,
        x_user_id=args.x_user_id,
        auto_post=args.auto_post,
        action=args.action
    )

    sim.regenerate_now()
    sim.run()

if __name__ == "__main__":
    main()
