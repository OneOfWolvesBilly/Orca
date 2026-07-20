FROM node:20

WORKDIR /workspace

COPY orca_frontend/packages/react-login ./orca_frontend/packages/react-login
COPY orca_frontend/react/package*.json ./orca_frontend/react/

WORKDIR /workspace/orca_frontend/react
RUN npm ci

COPY orca_frontend/react ./

EXPOSE 5173

CMD ["npm", "run", "dev", "--", "--host", "0.0.0.0", "--port", "5173"]
