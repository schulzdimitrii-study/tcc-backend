CREATE TABLE users (
   id UUID NOT NULL,
   email VARCHAR(255) NOT NULL UNIQUE,
   name VARCHAR(255) NOT NULL,
   password VARCHAR(255) NOT NULL,
   birthday_date DATE,
   max_heart_rate INTEGER,
   height DOUBLE PRECISION,
   weight DOUBLE PRECISION,
   PRIMARY KEY (id)
);
