from http.server import ThreadingHTTPServer,SimpleHTTPRequestHandler
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
class Handler(SimpleHTTPRequestHandler):
    def __init__(self,*a,**k):super().__init__(*a,directory=str(ROOT),**k)
    def do_GET(self):
        if self.path in ['/','/preview.html']:self.path='/review/preview.html'
        super().do_GET()
print('S01 review: http://127.0.0.1:8827/preview.html',flush=True)
ThreadingHTTPServer(('127.0.0.1',8827),Handler).serve_forever()
