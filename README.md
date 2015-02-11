# HttpSocketAdaptor
A Simple Http to Raw Socket Adapter for Android

HTTP Socket adaptor is a small utility that allows the communication with a TCP socket device from any android web browser.

It uses a HTTP server on localhost to receive data from ajax calls and pass them on to the specified host using a raw socket connection. I've created it to communicate with network printers directly from the web browser, and at this point it's only unidirectional. If you would like bi-directional support please let me know.

Once the drafted html RawSocket API becomes finalised this application will be redundant.

NOTE: When using in a HTTPS page, use the javascript example provided to avoid mixed content restrictions.
