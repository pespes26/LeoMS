# Player client preparation

LeoMS does not provide or link to a client download. Use only MapleStory v83 files you are legally entitled to possess. Keep the entire client directory outside the LeoMS repository and outside any cloud/shared folder used to publish the server source.

## Compatibility gate

Before changing anything, make a private backup of your original files. Confirm that the client identifies as Global MapleStory version 83 and that its locale matches Cosmic’s GMS v83 protocol. Record hashes locally so every player tests the same build; do not commit those files or bundle them with documentation.

```powershell
Get-FileHash .\MapleStory.exe -Algorithm SHA256
Get-ChildItem . -Filter *.wz | Get-FileHash -Algorithm SHA256
```

Configure your own client to connect to the stable Tailscale IPv4 supplied privately by the operator. The exact launcher/host-redirection method depends on the legitimately obtained client; LeoMS does not distribute patched executables or patching tools. Do not disable antivirus protections or run unknown third-party binaries.

Install Tailscale on the Windows machine, sign in with the identity approved by the operator, and verify that the server’s tailnet IP responds before launching. A nonexistent username must fail to log in; the operator creates accounts through the private admin panel and shares initial credentials out of band.

## macOS players

Use a Windows 10 or Windows 11 virtual machine. Install Tailscale inside the VM so the game process connects directly through the tailnet. Allocate enough RAM and graphics support for the client, keep Windows updated, and take a VM snapshot after the known-good client is configured.

Native Wine and CrossOver operation is unsupported. It may work in some environments, but it is not part of the compatibility or incident-response baseline.

## Required pre-launch test

On both Windows hardware and the macOS-hosted Windows VM, test login, character creation, both channels, channel switching, quests, party play, trading, cash shop entry/exit, disconnect/reconnect, PIN, and PIC. Stop if the client build behaves differently between players; resolve compatibility before a broader test.
