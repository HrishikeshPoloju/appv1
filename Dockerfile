# Root Dockerfile for Google Cloud Run monorepo root context
FROM golang:1.26-alpine AS builder

WORKDIR /app

ENV GOTOOLCHAIN=auto

RUN apk add --no-cache git ca-certificates

COPY backend-go/go.mod backend-go/go.sum ./
RUN go mod download

COPY backend-go/ ./

RUN CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build \
    -ldflags="-w -s" \
    -o /app/server ./cmd/server

FROM alpine:latest

WORKDIR /app

RUN apk --no-cache add ca-certificates tzdata

ENV TZ=Asia/Kolkata
ENV PORT=8080
ENV ENVIRONMENT=production

COPY --from=builder /app/server /app/server

EXPOSE 8080

ENTRYPOINT ["/app/server"]
