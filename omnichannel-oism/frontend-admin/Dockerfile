FROM node:22-alpine
WORKDIR /app
COPY package.json package-lock.json ./
# npm install (khong dung ci): lockfile sinh tren Windows lech goi tuy chon theo nen tang
RUN npm install --no-audit --no-fund
COPY . .
# NEXT_PUBLIC_* duoc nhung luc build, trinh duyet goi API qua dia chi nay
ARG NEXT_PUBLIC_API_URL=http://localhost:8080/api/v1
ENV NEXT_PUBLIC_API_URL=$NEXT_PUBLIC_API_URL
RUN npm run build
ENV NODE_ENV=production
EXPOSE 3000
CMD ["npm", "run", "start"]
