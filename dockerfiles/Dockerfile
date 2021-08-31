# mariadb -> mydb-image
# ~/dbdata -> copy -> init db
# environments -> root pw, db name
FROM mariadb:latest

ENV MARIADB_ROOT_PASSWORD 1234
ENV MARIADB_DATABASE mydb

COPY ./shared_dbdata /var/lib/mysql

EXPOSE 3306

CMD ["mysqld"]