FROM node:20-alpine

WORKDIR /app

COPY package.json .
RUN npm install --omit=dev

COPY server.js .
COPY mcp-stdio.js .

ENV PORT=5004
ENV PGHOST=postgres
ENV PGPORT=5432
ENV PGDATABASE=mydb
ENV PGUSER=postgres
ENV PGPASSWORD=password
ENV WEBAPP_URL=http://webapp:5001
ENV ANALYTICS_URL=http://analytics-service:5002
ENV EVENTS_URL=http://events-service:5003

EXPOSE 5004

CMD ["node", "server.js"]
