<#
Copy this file to scripts/release-config.ps1 and adjust non-secret deployment
settings. The local filename is ignored by Git. Never put passwords, tokens,
environment-file contents, or private-key contents in this file.
#>

@{
    SshHost                = '87.248.131.157'
    SshPort                = 9011
    SshUser                = 'root'
    # Leave null to use OpenSSH standard ~/.ssh discovery and/or ssh-agent.
    # IdentityFile         = "$HOME\.ssh\sami_vps_ed25519"
    RemoteArtifactDirectory = '/root'
    RemoteComposeDirectory  = '/root/sami-full-project/sami-backend'
    RemoteHistoryDirectory  = '/root/sami-deployment-history'
    ComposeFile             = 'docker-compose.prod.yml'
    EnvironmentFile         = '.env'
    BackendImageTag         = 'sami-backend:test'
    FrontendImageTag        = 'sami-frontend:test'
    FrontendApiBaseUrl      = '/api'
    ApplicationVersion      = '0.1.0'
    ApplicationUrl          = 'http://87.248.131.157'
}
