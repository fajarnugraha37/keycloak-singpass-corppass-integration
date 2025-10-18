FROM node:22-alpine
WORKDIR /app
COPY package*.json ./
RUN npm install 
COPY . .
RUN ls -al
EXPOSE 3000
CMD ["node","index.js"]
