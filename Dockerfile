# Root Dockerfile for Google Cloud Run continuous deployment from GitHub monorepo
FROM golang:1.24-alpine AS builder

WORKDIR /app

RUN apk add --no-cache git ca-certificates

# Copy Go module files from backend-go/
COPY backend-go/go.mod backend-go/go.sum ./
RUN go mod download

# Copy Go backend source code
COPY backend-go/ ./

# Build statically compiled Linux binary
RUN CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build \
    -ldflags="-w -s" \
    -o /app/server ./cmd/server

# Stage 2: Minimal runtime image for Cloud Run
FROM alpine:latest

WORKDIR /app

RUN apk --no-cache add ca-certificates tzdata

ENV TZ=Asia/Kolkata
ENV PORT=8080
ENV ENVIRONMENT=production

COPY --from=builder /app/server /app/server

EXPOSE 8080

ENTRYPOINT ["/app/server"]
