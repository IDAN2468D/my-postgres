FROM python:3.11-slim

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY main.py .
COPY multi_agent_orchestrator.py .

ENV PORT=5002
ENV PGHOST=postgres
ENV PGPORT=5432
ENV PGDATABASE=mydb
ENV PGUSER=postgres
ENV PGPASSWORD=password

EXPOSE 5002

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "5002"]
