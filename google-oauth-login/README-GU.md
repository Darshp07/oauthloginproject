# Google OAuth Login — Spring Boot Project

આ demo Spring Boot, Spring Security, OAuth 2.0 / OpenID Connect અને Thymeleaf વડે Google Login કરાવે છે.

## Requirements
- JDK 17 અથવા તેથી નવું
- Maven (અથવા Eclipse/STS માં Maven support)
- Google account
- Internet connection

## 1. Google Cloud માં OAuth Client બનાવો
1. https://console.cloud.google.com/ ખોલો અને project બનાવો/પસંદ કરો.
2. Google Auth Platform માં Branding/Consent Screen configure કરો.
3. Audience માં app testing mode માં હોય તો તમારું Google account Test Users માં ઉમેરો.
4. Clients માં OAuth client બનાવો અને Application type `Web application` પસંદ કરો.
5. Authorized redirect URI તરીકે ચોક્કસ આ ઉમેરો:
   `http://localhost:8080/login/oauth2/code/google`
6. Client ID અને Client Secret copy કરો. Secret જાહેર ન કરો.

Google Console માં menu labels account/UI પ્રમાણે થોડા અલગ હોઈ શકે.

## 2. Project ચલાવો
1. ZIP extract કરો.
2. Eclipse માં `File > Import > Maven > Existing Maven Projects` પસંદ કરો.
3. Extract કરેલા folder ને Root Directory તરીકે પસંદ કરો અને Finish કરો.
4. Google Cloud માંથી મળેલા credentials environment variables તરીકે સેટ કરો.

Windows Command Prompt માં, એ જ terminal session માં:
```bat
set GOOGLE_CLIENT_ID=your-client-id
set GOOGLE_CLIENT_SECRET=your-client-secret
set DB_USERNAME=root
set DB_PASSWORD=your-database-password
set JWT_SECRET_KEY=your-base64-encoded-random-32-byte-key
mvn spring-boot:run
```

PowerShell માં:
```powershell
$env:GOOGLE_CLIENT_ID="your-client-id"
$env:GOOGLE_CLIENT_SECRET="your-client-secret"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your-database-password"
$env:JWT_SECRET_KEY="your-base64-encoded-random-32-byte-key"
mvn spring-boot:run
```

Eclipse માં Run Configurations > Spring Boot App > Environment tab માં આ પાંચ
variables ઉમેરો: `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `DB_USERNAME`,
`DB_PASSWORD`, અને `JWT_SECRET_KEY`. JWT key Base64 encoded random value હોવી
જોઈએ અને તેમાં ઓછામાં ઓછા 32 bytes હોવા જોઈએ.

બંને Google credentials એ જ Google `Web application` client ના હોવા જોઈએ.
`application.properties` માં secrets નથી; credentials માત્ર environment માં
રાખો. પહેલાં source control માં ઉમેરાયેલા OAuth client secrets અથવા JWT keys
હોય તો તેને revoke/rotate કરો. પછી Spring Boot app stop કરીને ફરી start કરો.

પછી application run કરો.

## 3. Test કરો
1. http://localhost:8080 ખોલો.
2. `Continue with Google` click કરો.
3. Google account પસંદ કરો અને permission approve કરો.
4. Home page પર નામ/email દેખાશે.
5. `Open Dashboard` પર click કરો.
6. Logout કરો અને ફરી `/dashboard` ખોલો. Login વગર page access ન થવું જોઈએ.

## Flow સરળ ભાષામાં
- `/oauth2/authorization/google`: Spring Security Google login શરૂ કરે છે.
- Google user ને authenticate કરે છે.
- Google browser ને `/login/oauth2/code/google` પર પાછું મોકલે છે અને authorization code આપે છે.
- Spring Security code ને tokens માં exchange કરે છે અને OIDC user information મેળવે છે.
- પ્રથમ verified Google login પર backend database માં user બનાવે છે અથવા
  હાલના user account સાથે verified email દ્વારા link કરે છે.
- નવા user ને માત્ર `ROLE_USER` મળે છે. `ROLE_ADMIN` ફક્ત trusted user ને
  database માંથી manually આપો; registration request માંથી નહીં.
- React access token સાથે `/api/auth/appUsers/me` દ્વારા user role અને status
  મેળવે છે. Backend દરેક API request પર database માંથી user, role અને status
  ફરી ચકાસે છે.
- `GetAllUsers` અને account status બદલવાનું API `@PreAuthorize("hasRole('ADMIN')")`
  થી સુરક્ષિત છે. React માં admin page છુપાવવું માત્ર UX છે; backend rule જ
  વાસ્તવિક સુરક્ષા આપે છે.
- Suspended account login/API access કરી શકતો નથી. Admin user page માંથી
  account ફરી `ACTIVE` કરી શકે છે; પોતાનું account suspend કરી શકતો નથી.

## Database અને role setup

Backend `ddl-auto=validate` વાપરે છે. Updated app પ્રથમ વખત ચલાવતાં પહેલાં
જૂના database માં આ ફેરફારો કરો. `ALTER TABLE` એક જ વખત ચલાવો; role inserts
ફરી ચલાવી શકાય છે.

```sql
ALTER TABLE app_users
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

INSERT IGNORE INTO roles (role_name)
VALUES ('ROLE_USER'), ('ROLE_ADMIN');

INSERT IGNORE INTO user_roles (user_id, role_id)
SELECT app_users.id, roles.id
FROM app_users
JOIN roles ON roles.role_name = 'ROLE_USER';
```

પ્રથમ administrator માટે, `admin@example.com` ને trusted account emailથી
બદલીને આ query ચલાવો:

```sql
INSERT IGNORE INTO user_roles (user_id, role_id)
SELECT app_users.id, roles.id
FROM app_users
JOIN roles ON roles.role_name = 'ROLE_ADMIN'
WHERE LOWER(app_users.email) = LOWER('admin@example.com');
```

## Important
- Google Login માટે OAuth 2.0 સાથે OpenID Connect (OIDC) વપરાય છે. Authorization
  Server frontend ને PKCE access token આપે છે.
- OAuth Client Secret, database password અને JWT signing key source control/GitHub
  પર commit ન કરો.
- `localhost` setup local learning માટે છે. Production માં HTTPS, secure cookie/session settings, correct redirect URIs, secret management અને account-linking/security review જરૂરી છે.
- Google account profile/email data ઉપલબ્ધ હોય તે scope/consent પર આધારિત છે.
