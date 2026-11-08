FROM node:22-alpine
WORKDIR /srv/app
COPY package.json ./
RUN npm install --omit=dev --no-audit --no-fund
COPY src ./src
ENV NODE_ENV=production
EXPOSE 8080
USER node
CMD ["node", "src/server.js"]
