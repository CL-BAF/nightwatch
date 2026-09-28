"""Local speech-to-text endpoint. Raw sound stays in memory and never leaves this computer."""
import io
import json
import os
from http.server import BaseHTTPRequestHandler, HTTPServer

from faster_whisper import WhisperModel

model = WhisperModel(os.getenv("NIGHTWATCH_WHISPER_MODEL", "base.en"), device="cpu", compute_type="int8")


class Handler(BaseHTTPRequestHandler):
    def do_POST(self):
        length = int(self.headers.get("Content-Length", "0"))
        if self.path != "/transcribe" or not (44 <= length <= 100_000):
            self.send_error(400)
            return
        wav = io.BytesIO(self.rfile.read(length))
        try:
            segments, _ = model.transcribe(wav, beam_size=1, language="en", vad_filter=True)
            text = " ".join(segment.text.strip() for segment in segments)[:180]
            body = json.dumps({"text": text}).encode("utf-8")
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
        except Exception:
            self.send_error(422)

    def log_message(self, format, *args):
        pass  # Transcripts and raw audio are never logged.


if __name__ == "__main__":
    print("Nightwatch local transcription ready on 127.0.0.1:8765")
    HTTPServer(("127.0.0.1", 8765), Handler).serve_forever()
