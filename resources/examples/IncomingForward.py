import string, cgi, time
import sys
from BaseHTTPServer import BaseHTTPRequestHandler, HTTPServer

number =""

class MyHandler(BaseHTTPRequestHandler):

    def do_GET(self):
        println("not supported") 
        return

    def do_POST(self):
        if self.path == "/Status":
            self.handleStatus()
        else: 
            self.handleIncomingCall()   

    def handleStatus(self):
        params = self.parseParams()
        for field in params.keys():
            print( field + "=" + "".join(params[field]))
    
    def handleIncomingCall(self):
        self.send_response(200)
        self.send_header("Content-type", "text/html")
        self.end_headers()
        str = """
                <Response>
                    <Dial>
                        <Number>
                        """ + number + """
                        </Number>
                        <Action>http://127.0.0.1:8081/Status</Action>
                    </Dial>
                </Response>
            """
        self.wfile.write(str)
        return
    
    def parseParams(self):
        length = int(self.headers.getheader('Content-Length'))
        params = cgi.parse_qs(self.rfile.read(length), keep_blank_values=1)
        return params
    


def main():
    try:
        server = HTTPServer( ('', 8081), MyHandler)
        print("started, forwarding to " + number)
        server.serve_forever()
        print("serving...")
        #time.sleep(5000)
    except Exception, err:
        print("damn error = " + str(err))

if __name__ == '__main__':
    number = sys.argv[1]
    main()
