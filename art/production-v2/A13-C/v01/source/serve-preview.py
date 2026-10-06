from pathlib import Path
from http.server import ThreadingHTTPServer,BaseHTTPRequestHandler
from urllib.parse import urlparse
import json,hashlib
R=Path(__file__).resolve().parents[1]
N={a['url']:a for a in json.loads((R/'references/native-local-map.json').read_text())['assets']}
class Handler(BaseHTTPRequestHandler):
 def do_GET(self):
  path=urlparse(self.path).path
  if path in ['/', '/preview.html']:p=R/'review/preview.html';mime='text/html; charset=utf-8'
  elif path in N:
   a=N[path];p=Path(a['path']);mime='audio/ogg'
   if not p.is_file() or hashlib.sha256(p.read_bytes()).hexdigest()!=a['sha256']:self.send_error(503,'Local game reference unavailable');return
  else:self.send_error(404);return
  data=p.read_bytes();self.send_response(200);self.send_header('Content-Type',mime);self.send_header('Content-Length',str(len(data)));self.send_header('Cache-Control','no-store');self.end_headers();self.wfile.write(data)
 def log_message(self,*args):pass
if __name__=='__main__':
 print('A13-C preview http://127.0.0.1:8830/preview.html',flush=True)
 ThreadingHTTPServer(('127.0.0.1',8830),Handler).serve_forever()
