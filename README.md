# OwnerClanPlugin

Plugin de Paper (Minecraft 26.2, build 116) hecho a medida con:

- **Acceso total automático** para la cuenta `Cuticu098` (se le da op en cuanto entra → puede usar todos los comandos).
- **Tags de chat personalizados** por jugador (`/tag`), con color normal, hex (`#RRGGBB`) o **arcoíris**. El owner sale por defecto como `[OWNER] Cuticu098` en arcoíris.
- **Sistema de clanes completo** (`/clan`): crear, disolver, invitar, expulsar, ascender/degradar oficiales, tag propio del clan, permisos configurables por rol, ranking por kills.
- **Integración con Discord**: noticias de clanes (creación, disolución, ranking de kills cada X minutos) y un log privado de acciones de administración, cada uno a su propio webhook.

## ⚠️ Importante sobre los webhooks

Los dos enlaces de Discord que me diste ya están puestos por defecto en `src/main/resources/config.yml`. Cualquiera que tenga esos enlaces puede publicar mensajes en tus canales de Discord, así que no los compartas en ningún sitio público (ni los subas a un repo público de GitHub sin quitarlos antes).

## Cómo compilar el plugin

Yo no puedo compilar el `.jar` desde aquí (mi entorno no tiene acceso al repositorio Maven de PaperMC), así que tienes dos opciones:

### Opción A: Con GitHub Actions (no necesitas instalar nada)
1. Crea un repositorio en GitHub (puede ser privado) y sube esta carpeta entera (ya incluye `.github/workflows/build.yml`).
2. Ve a la pestaña **Actions** del repo → se ejecutará solo → cuando termine, descarga el artefacto `OwnerClanPlugin` desde esa misma ejecución. Ahí está tu `OwnerClanPlugin.jar`.

### Opción B: En tu PC con Maven
1. Instala JDK 21+ y Maven.
2. Abre una terminal en esta carpeta y ejecuta:
   ```
   mvn package
   ```
3. El jar compilado queda en `target/OwnerClanPlugin.jar`.

## Cómo instalarlo en Aternos

1. Entra al panel de tu servidor en Aternos → **Archivos** → carpeta `plugins`.
2. Sube `OwnerClanPlugin.jar` ahí.
3. Reinicia el servidor.
4. Se generará `plugins/OwnerClanPlugin/config.yml` — revisa que `owner-name` sea exactamente `Cuticu098` y que los webhooks sean correctos.
5. Entra al servidor con la cuenta `Cuticu098`: recibirás el mensaje de acceso total automáticamente.

## Comandos

### `/tag` (owner o permiso `ownerclan.tag.set`)
- `/tag set <jugador> <color|rainbow> <texto>`
- `/tag remove <jugador>`
- `/tag list`

### `/clan`
- `/clan create <nombre> <color> [texto del tag]`
- `/clan invite <jugador>` · `/clan accept` · `/clan deny`
- `/clan kick <jugador>` · `/clan leave` · `/clan disband`
- `/clan tag <color> <texto>`
- `/clan promote <jugador>` · `/clan demote <jugador>`
- `/clan perm <invite|kick|tag|promote|demote> <true|false>` (solo el líder decide qué pueden hacer los oficiales)
- `/clan info [nombre]` · `/clan list` · `/clan top [n]`

## Notas
- Los datos de clanes y tags se guardan en `plugins/OwnerClanPlugin/clans.yml` y `tags.yml`.
- El ranking automático a Discord se manda cada `leaderboard-interval-minutes` (30 por defecto, configurable, 0 para desactivarlo).
- Si algún día cambias de cuenta owner, solo edita `owner-name` en `config.yml` y reinicia.
