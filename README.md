# Akka HTTP Examples on Streaming Requests and Responses

This project is a showcase of 2 simple examples on how to perform streaming requests and responses with Akka HTTP. 
This does not make use of [Entity Streaming Support](http://doc.akka.io/docs/akka-http/10.0.5/scala/http/routing-dsl/source-streaming-support.html#source-streaming)
and interacts with ByteStrings and hand-parses text using `Framing.delimiter` in order to go from a `ByteString` into 
something that is easier to work with (like `String`).

There are two examples: 

- A GET endpoint that returns a streaming text response (`/batch2`) 
- A POST endpoint that requires streaming text as input and produces streaming text as output, it takes a string as 
input and produces the UUID equivalent of the string as output (`/batch`)

The client shows how to interact with both endpoints. It pulls some example strings from a file and sends them through 
to the server and prints out the UUIDs.