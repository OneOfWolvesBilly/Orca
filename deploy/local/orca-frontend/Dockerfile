FROM node:20

WORKDIR /workspace/orca_frontend/react

COPY orca_frontend/react/package*.json ./
RUN npm ci

COPY orca_frontend/react ./

EXPOSE 5173

CMD ["npm", "run", "dev", "--", "--host", "0.0.0.0", "--port", "5173"]
