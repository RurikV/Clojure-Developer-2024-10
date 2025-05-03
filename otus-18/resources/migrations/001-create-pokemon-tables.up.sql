-- Create types table
CREATE TABLE IF NOT EXISTS types (
  id SERIAL PRIMARY KEY,
  name VARCHAR(255) NOT NULL UNIQUE,
  localized_name VARCHAR(255)
);

-- Create pokemons table
CREATE TABLE IF NOT EXISTS pokemons (
  id SERIAL PRIMARY KEY,
  name VARCHAR(255) NOT NULL UNIQUE,
  url VARCHAR(255)
);

-- Create pokemon_types join table
CREATE TABLE IF NOT EXISTS pokemon_types (
  pokemon_id INTEGER REFERENCES pokemons(id) ON DELETE CASCADE,
  type_id INTEGER REFERENCES types(id) ON DELETE CASCADE,
  PRIMARY KEY (pokemon_id, type_id)
);