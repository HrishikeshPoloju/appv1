package notifications

import (
	"context"
	"fmt"
	"log/slog"
	"os"
	"strings"

	firebase "firebase.google.com/go/v4"
	"firebase.google.com/go/v4/messaging"
	"github.com/schoolapp/backend-go/internal/models"
	"google.golang.org/api/option"
)

type FCMService struct {
	client *messaging.Client
}

func NewFCMService(ctx context.Context, credsPath string) (*FCMService, error) {
	var opts []option.ClientOption
	if credsPath != "" {
		if _, err := os.Stat(credsPath); err == nil {
			opts = append(opts, option.WithCredentialsFile(credsPath))
		}
	}

	app, err := firebase.NewApp(ctx, nil, opts...)
	if err != nil {
		return nil, fmt.Errorf("failed to initialize Firebase app: %w", err)
	}

	msgClient, err := app.Messaging(ctx)
	if err != nil {
		return nil, fmt.Errorf("failed to initialize FCM messaging client: %w", err)
	}

	slog.Info("Firebase Cloud Messaging (FCM) initialized successfully (free unlimited push)")
	return &FCMService{client: msgClient}, nil
}

// SchoolNoticeTopic returns the canonical FCM topic name for a school's notices.
func SchoolNoticeTopic(schoolID string) string {
	clean := strings.ToLower(strings.TrimSpace(schoolID))
	clean = strings.ReplaceAll(clean, "-", "_")
	if clean == "" {
		clean = "00000000_0000_0000_0000_000000000001"
	}
	return "school_" + clean
}

// SendSchoolNotice broadcasts a free push notification to all devices subscribed to that school's topic.
func (s *FCMService) SendSchoolNotice(
	ctx context.Context,
	schoolID string,
	category string,
	title string,
	content string,
	postID string,
) (string, error) {
	if s == nil || s.client == nil {
		return "", fmt.Errorf("FCM service not initialized")
	}

	topic := SchoolNoticeTopic(schoolID)
	notifTitle := strings.TrimSpace(title)
	if category != "" {
		notifTitle = fmt.Sprintf("[%s] %s", strings.ToUpper(category), notifTitle)
	}
	notifBody := strings.TrimSpace(content)
	if len(notifBody) > 220 {
		notifBody = notifBody[:217] + "..."
	}

	msg := &messaging.Message{
		Topic: topic,
		Notification: &messaging.Notification{
			Title: notifTitle,
			Body:  notifBody,
		},
		Android: &messaging.AndroidConfig{
			Priority: "high",
			Notification: &messaging.AndroidNotification{
				ChannelID: "school_notices",
				Sound:     "default",
				Color:     "#1E40AF",
			},
		},
		Data: map[string]string{
			"type":      "NOTICE",
			"post_id":   postID,
			"category":  category,
			"school_id": schoolID,
			"title":     notifTitle,
			"body":      notifBody,
		},
	}

	msgID, err := s.client.Send(ctx, msg)
	meta := models.GetLogMeta(ctx)
	if err != nil {
		if meta != nil {
			meta.AddGCSOp(fmt.Sprintf("FCM Notice push FAILED for topic '%s': %v", topic, err))
		}
		return "", err
	}

	if meta != nil {
		meta.AddGCSOp(fmt.Sprintf("Sent FREE FCM Notice push to topic '%s' (message_id: %s)", topic, msgID))
	}
	return msgID, nil
}
