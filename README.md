<div align="center">
  <h1>Multi Instance Meteor</h1>
  <p>Easily launch new instances of Minecraft in a quick and configurable way.</p>
</div>

Features:
- Account Authentication
- Configurable JRE/JDK
- Modify Min/Max RAM
- Append JVM Options

CLI Args: 
- `--meteor:swarmMode`: `worker` or `host`
- `--meteor:swarmPort`: int
- `--meteor:swarmIp`: String, only if `worker`
- `--meteor:deactivate`: Runs `toggle all off` on startup
- `--meteor:joinServer`: Join server immediately after launching
- `--meteor:serverIp`: String, only if `joinServer`
