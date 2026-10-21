FROM node:20-alpine

WORKDIR /app

COPY package.json .
RUN npm install --omit=dev

COPY server.js .

ENV PORT=5003
ENV REDIS_URL=redis://redis:6379
ENV PGHOST=postgres
ENV PGPORT=5432
ENV PGDATABASE=mydb
ENV PGUSER=postgres
ENV PGPASSWORD=password

EXPOSE 5003

CMD ["node", "server.js"]
