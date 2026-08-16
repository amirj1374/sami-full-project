-- Persist the per-user keyboard shortcut preference introduced by the
-- application-wide keyboard navigation layer. Existing users opt in by
-- default and can disable the feature from their profile settings.
ALTER TABLE user_experience_preferences
    ADD COLUMN keyboard_shortcuts_enabled BOOLEAN NOT NULL DEFAULT TRUE;
