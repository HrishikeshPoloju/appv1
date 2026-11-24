# Multi-stage Dockerfile for Genesis School SaaS Go Backend
# Optimized for Google Cloud Run (High throughput, minimal attack surface)

# Stage 1: Build the Go binary
FROM golang:1.24-alpine AS builder

WORKDIR /app

# Install git and SSL certificates
RUN apk add --no-cache git ca-certificates

# Cache dependencies
COPY go.mod go.sum ./
RUN go mod download

# Copy source code
COPY . .

# Build statically compiled binary without CGO
RUN CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build \
    -ldflags="-w -s" \
    -o /app/server cmd/server/main.go

# Stage 2: Final lightweight image
FROM alpine:latest

WORKDIR /app

RUN apk --no-cache add ca-certificates tzdata

# Set timezone to Asia/Kolkata for Indian school operations
ENV TZ=Asia/Kolkata

# Copy compiled binary from builder
COPY --from=builder /app/server /app/server

# Expose port (Cloud Run defaults to 8080)
EXPOSE 8080

# Environment variables for Cloud Run
ENV PORT=8080
ENV ENVIRONMENT=production

# Run binary
ENTRYPOINT ["/app/server"]
