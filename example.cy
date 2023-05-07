print 1, 2, 3

fn http_get(url)
  var u = jvm.cls('java.net.URI')('https://api.ipify.org')
  var i = u.toURL().openStream()
  var bytes = i.readAllBytes()
  var contents = jvm.cls('java.lang.String')(bytes)
  i.close()
  return contents

print http_get('https://api.ipify.org'), http.get('https://api.ipify.org')