FROM node:18-alpine

WORKDIR /usr/src/mockpass

COPY package* ./

COPY ./.husky ./.husky

RUN npm install

COPY . ./

CMD ["node", "index.js"]
