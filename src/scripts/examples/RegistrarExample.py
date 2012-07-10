import string, cgi, time
import sys
from BaseHTTPServer import BaseHTTPRequestHandler, HTTPServer


class MyHandler(BaseHTTPRequestHandler):

    params = {}
   
    lookup = { '':''}

    
    def do_GET(self):
        self.do_POST()

    def do_POST(self):
        self.parseParams()
        print("  Incoming Path = " + self.path)
        if self.path == "/register/":
            print("ok we're responding with stuff")
            self.handleRegister()
            self.printParams()
        else:
            self.printParams()
            self.handleIncomingCall()   

    def printParams(self):
        for field in self.params.keys():
            print( field + "=" + "".join(self.params[field]))

    def getParam(self,name):
        return "".join(self.params[name])

    def postOK(self):
        self.send_response(200)
        self.send_header("Content-Type", "text/html")
        self.end_headers()

    def handleRegister(self):
        self.postOK()
        authType = self.getParam("AuthType")
        print("authtype = " + authType)
        if authType == "Request":
            print("HERE")
            s = """
                <Response>
                    <Auth>
                        <Password>asdf</Password>
                    </Auth>
                </Response>
                """
            self.wfile.write(s)
        else:
            print("Setting " + self.getParam("RegisterAddress") + " = " + self.getParam("ContactAddress"))    
            self.lookup[self.getParam("RegisterAddress")] = self.getParam("ContactAddress")
            self.wfile.write("<html> </Html>")
        return
        
    def handleIncomingCall(self):
        self.postOK()
        print("IN HANDLE INCOMING CALL looking for " + self.getParam("To"))
        print("lookup is = " + self.lookup[self.getParam("To")])
        str = """
                <Response>
                    <Dial>
                        <Number>""" + self.lookup[self.getParam("To")] + """</Number>
                        <From>""" + self.getParam("From") + """ </From>
                        <Action>http://127.0.0.1:8081/Status</Action>
                    </Dial>
                </Response>
            """
        
        print( "responding with "+ str )
        self.wfile.write(str)
        return

    def parseParams(self):
        print("content-length = " + self.headers.getheader('Content-Length'))
        length = int(self.headers.getheader('Content-Length'))
        self.params = cgi.parse_qs(self.rfile.read(length), keep_blank_values=1)


def main():
    try:
        server = HTTPServer( ('', 8081), MyHandler)
        print("started, we're a register server")
        server.serve_forever()
        print("serving...")
        #time.sleep(5000)
    except Exception, err:
        print("damn error = " + str(err))

if __name__ == '__main__':
    main()

