CREATE TABLE "chat" (
    "id" serial,
    "name" varchar(255),
    primary key ("id")
);

CREATE TABLE "message" (
    "id" serial,
    "chat_id" bigint not null,
    "author" varchar(255),
    "content" text,
    primary key("id")
);

ALTER TABLE IF EXISTS "message"
    ADD CONSTRAINT "fk_message_chat"
        FOREIGN KEY("chat_id")
            REFERENCES "chat";