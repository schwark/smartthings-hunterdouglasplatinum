import socket
import sys

HD_GATEWAY_PORT = 522
TIMEOUT = 10

def create_socket(server):
  try:
    sock = socket.create_connection((server, HD_GATEWAY_PORT), timeout=TIMEOUT)
    helo = recv_until(sock, 'Shade Controller')
  except socket.error:
    sock.close()
    sock = None
  return sock

def socket_com(server, message, sentinel=None, sock=None):
  content = None
  try:
    if not sock:
      sock = create_socket(server)
      sock.sendall(message)
      content = recv_until(sock, sentinel)
  except socket.error:
    pass
  finally:
    if sock:
      sock.close()
  return content

def recv_until(sock, sentinel=None):
  info = ""
  while True:
    try:
      chunk = sock.recv(1)
    except socket.timeout:
      break
    info += chunk
    if info.endswith(sentinel): break
    if not chunk: break
  return info

def get_status(server):
	return socket_com(server, "$dat", "upd01-")

def main():
	if(len(sys.argv) < 2):
		print "Usage: ",sys.argv[0],"<gateway-ip>"
	else:
		print get_status(sys.argv[1])
	

if __name__ == "__main__":
    main()
  