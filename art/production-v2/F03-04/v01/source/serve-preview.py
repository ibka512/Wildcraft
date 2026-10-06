from http.server import ThreadingHTTPServer,SimpleHTTPRequestHandler
from pathlib import Path
R=Path(__file__).resolve().parents[1]
class Handler(SimpleHTTPRequestHandler):
 def __init__(self,*args,**kwargs):super().__init__(*args,directory=str(R),**kwargs)
 def do_GET(self):
  if self.path in ('/','/preview.html'):self.path='/review/preview.html'
  super().do_GET()
 def log_message(self,*args):pass
print('Review available on http://127.0.0.1:8826/preview.html',flush=True)
ThreadingHTTPServer(('127.0.0.1',8826),Handler).serve_forever()
