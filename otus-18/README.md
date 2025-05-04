# Pokemon Application with Hexagonal Architecture

This project demonstrates the use of hexagonal architecture (ports and adapters) in a Clojure application that interacts with the Pokemon API and stores data in a PostgreSQL database.

## Architecture Overview

The application is structured according to the principles of hexagonal architecture:

### Domain Layer
- Core business logic and entities
- Domain protocols (ports)

### Application Layer
- Use cases that orchestrate domain entities
- Services that implement business operations

### Infrastructure Layer
- External systems implementations (API client, database repository)
- Adapters for external systems

### Boundary Layer
- Adapters that connect the application to external systems

## Technologies Used

- [Clojure](https://clojure.org/) - Programming language
- [Integrant](https://github.com/weavejester/integrant) - Dependency injection and lifecycle management
- [Duct](https://github.com/duct-framework/duct) - Application framework
- [next.jdbc](https://cljdoc.org/d/com.github.seancorfield/next.jdbc/1.3.874/doc/readme) - Database access
- [ragtime](https://github.com/weavejester/ragtime) - Database migrations
- [honeysql](https://cljdoc.org/d/com.github.seancorfield/honeysql/2.4.1026/doc/readme) - SQL query builder
- [core.async](https://github.com/clojure/core.async) - Asynchronous programming
- [clj-http](https://github.com/dakrone/clj-http) - HTTP client
- [PostgreSQL](https://www.postgresql.org/) - Database

## Setup and Configuration

### Database Setup
1. Install PostgreSQL if not already installed
2. Create a database:
   ```sql
   CREATE DATABASE pokemon_db;
   ```
3. For testing, create a test database:
   ```sql
   CREATE DATABASE pokemon_test_db;
   ```

### Environment Configuration
The application uses different configurations for development, production, and testing environments:

- **Development**: Uses a local database and limits API requests
- **Production**: Uses environment variables for database connection and has higher API request limits
- **Testing**: Uses mock implementations for external dependencies

## Running the Application

### Development Mode

First, make sure you have all the required dependencies:

```bash
# Install dependencies
lein deps
```

Then start a REPL:

```bash
# Start a REPL
lein repl

# In the REPL
user=> (require 'dev)
user=> (in-ns 'dev)
dev=> (go)  # Start the system

# Load Pokemon data
dev=> (def service (get system :otus-18.application/service))
dev=> (require '[otus-18.application.pokemon-service :as service])
dev=> (service/load-pokemons-to-db! service 10 "en")

# Query data
dev=> (service/get-pokemons-by-type service "electric")
```

If you encounter any errors about missing dependencies, make sure you're using the latest version of the project.clj file which includes all necessary development dependencies.

### Production Mode
```bash
# Set environment variables
export DATABASE_URL=jdbc:postgresql://localhost:5432/pokemon_db?user=postgres&password=pwd

# Run the application
lein run
```

### Testing
```bash
# Run all tests
lein test

# Run specific tests
lein test otus-18.application.pokemon-service-test
```

## API Examples

### Get Pokemon by Name
```clojure
(require '[otus-18.application.pokemon-service :as service])
(def system (integrant.repl/system))
(def pokemon-service (get system :otus-18.application/service))

(service/get-pokemon pokemon-service "pikachu")
```

### Get Pokemons by Type
```clojure
(service/get-pokemons-by-type pokemon-service "electric")
```

### Load Pokemons to Database
```clojure
(service/load-pokemons-to-db! pokemon-service 50 "en")
```

## Testing with Mock Implementations

The application uses dependency injection to make testing easier. In tests, mock implementations of the API client and repository are used:

```clojure
(def config
  {:otus-18.infrastructure.api/pokemon-api-mock
   {:mock-data {"pikachu" ["electric"]}}

   :otus-18.infrastructure.db/pokemon-repository-mock
   {:mock-data {:pokemons {"pikachu" {:name "pikachu"}}}}

   :otus-18.application/service
   {:repository #ig/ref :otus-18.domain/repository
    :api-adapter #ig/ref :otus-18.boundary.api/adapter}})

(def system (ig/init config))
(def service (get system :otus-18.application/service))

(service/get-pokemon service "pikachu")
```

## Project History

This project evolved through several stages:

1. **Initial Version**: Asynchronous interaction with the Pokemon API
2. **Database Integration**: Added PostgreSQL storage and query capabilities
3. **Hexagonal Architecture**: Restructured the code according to hexagonal architecture principles
4. **Dependency Injection**: Added Integrant and Duct for dependency injection and configuration
