# Virtual Threads and Asynchronous HTTP Calls

This project demonstrates the use of Java's virtual threads and asynchronous HTTP calls using the `HttpClient` API, `Mutiny`, and `CompletableFuture`. It showcases various execution strategies, including synchronous, asynchronous without virtual threads, and asynchronous with virtual threads.

## Features

- **Synchronous HTTP Calls**: Makes synchronous requests and measures execution time.
- **Asynchronous Calls without Virtual Threads**: Uses a fixed thread pool for asynchronous requests.
- **Asynchronous Calls with Virtual Threads**: Utilizes Java's virtual threads for improved scalability.
- **Mutiny Integration**: Leverages Mutiny for reactive programming with an easy-to-use API.
- **Combined Virtual Threads and Mutiny**: Demonstrates the synergy of virtual threads with reactive programming.

## Requirements

- Java 21 or later
- Dependencies:
    - [SmallRye Mutiny](https://smallrye.io/smallrye-mutiny/)
    - [Lombok](https://projectlombok.org/)

## Installation

1. Clone the repository:

   ```bash
   git clone https://github.com/yourusername/virtual-threads-http-client.git
   cd virtual-threads-http-client
   ```

2. Build the project using Maven:

   ````maven
   mvn clean install
   ````

## Usage

1. Run the application:
    ````
   java -jar target/your-artifact-name.jar

Replace your-artifact-name.jar with the name of the built JAR file.

2. The application will perform 100 HTTP GET requests to https://jsonplaceholder.typicode.com/users 
and log the response lengths and execution times for each variant.

## Code Structure
- **App.java**: Contains the main class with various methods to demonstrate different execution strategies.

   - **runSynchronously**: Synchronous HTTP requests.
   - **runWithPlatformThreads**: Asynchronous requests using a fixed thread pool. 
   - **runWithVirtualThreads**: Asynchronous requests using virtual threads. 
   - **runWithMutiny**: Asynchronous processing using Mutiny. 
   - **runWithVirtualThreadsMutiny**: Combining virtual threads with Mutiny.
