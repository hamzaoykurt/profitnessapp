param(
    [string] $ProjectRef = "dkcriptafzdrynsilxku",
    [string] $LogoUrl = "https://dkcriptafzdrynsilxku.supabase.co/functions/v1/email-assets/logo.png",
    [string] $TemplateDir = "$PSScriptRoot/templates",
    [switch] $WriteOnly
)

$ErrorActionPreference = "Stop"

function New-ProfitnessEmail {
    param(
        [string] $Preheader,
        [string] $Eyebrow,
        [string] $Title,
        [string] $BodyHtml,
        [string] $ActionHtml,
        [string] $AfterHtml = ""
    )

    $template = @'
<!doctype html>
<html lang="tr">
  <head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="color-scheme" content="dark light">
    <meta name="supported-color-schemes" content="dark light">
    <title>__TITLE__</title>
    <style>
      @media only screen and (max-width: 620px) {
        .pf-shell { width: 100% !important; }
        .pf-hero { padding: 26px 20px 22px 20px !important; }
        .pf-card { padding: 28px 20px 30px 20px !important; }
        .pf-logo { width: 78px !important; height: 78px !important; }
        .pf-brand { font-size: 28px !important; line-height: 32px !important; }
        .pf-code { font-size: 40px !important; letter-spacing: 10px !important; }
        .pf-title { font-size: 30px !important; line-height: 36px !important; }
        .pf-pill { display:block !important; margin:8px 0 0 0 !important; text-align:center !important; }
      }
    </style>
  </head>
  <body style="margin:0; padding:0; background:#050508; font-family:Inter, Arial, Helvetica, sans-serif; color:#F8F8F8;">
    <div style="display:none; max-height:0; overflow:hidden; opacity:0; color:transparent;">__PREHEADER__</div>
    <table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0" style="background-color:#050508; background-image:radial-gradient(circle at 14% 0%, rgba(203,255,77,.16), transparent 34%), radial-gradient(circle at 94% 12%, rgba(0,229,211,.16), transparent 30%), linear-gradient(180deg,#111117 0%,#050508 78%);">
      <tr>
        <td align="center" style="padding:28px 14px 34px 14px;">
          <table role="presentation" class="pf-shell" width="600" cellspacing="0" cellpadding="0" border="0" style="width:600px; max-width:600px;">
            <tr>
              <td style="border-radius:34px; background:#111117; border:1px solid #2A2A35; box-shadow:0 28px 72px rgba(0,0,0,.55); overflow:hidden;">
                <table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0">
                  <tr>
                    <td class="pf-hero" style="padding:34px 34px 28px 34px; background-color:#15151D; background-image:linear-gradient(135deg,rgba(203,255,77,.18),rgba(0,229,211,.10) 42%,rgba(255,107,107,.14) 100%); border-bottom:1px solid #2A2A35;">
                      <table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0">
                        <tr>
                          <td valign="middle" style="width:100px;">
                            <img class="pf-logo" src="__LOGO_URL__" width="92" height="92" alt="Profitness logo" style="display:block; width:92px; height:92px; border-radius:25px; box-shadow:0 16px 38px rgba(0,0,0,.46);">
                          </td>
                          <td valign="middle" style="padding-left:18px;">
                            <div class="pf-brand" style="font-size:32px; line-height:36px; font-weight:900; letter-spacing:0; color:#FFFFFF;">Profitness</div>
                            <div style="margin-top:3px; font-size:12px; line-height:17px; color:#D9D9E6; font-weight:800; letter-spacing:2px;">BY AVONIX</div>
                            <div style="margin-top:13px;">
                              <span class="pf-pill" style="display:inline-block; padding:8px 12px; border-radius:999px; background:#CBFF4D; color:#0A0A0F; font-size:11px; line-height:13px; font-weight:900;">GÜVENLİ DOĞRULAMA</span>
                              <span class="pf-pill" style="display:inline-block; margin-left:7px; padding:8px 12px; border-radius:999px; border:1px solid rgba(255,255,255,.22); color:#F8F8F8; font-size:11px; line-height:13px; font-weight:800;">PROFITNESS APP</span>
                            </div>
                          </td>
                        </tr>
                      </table>
                    </td>
                  </tr>
                  <tr>
                    <td class="pf-card" style="padding:38px 36px 34px 36px;">
                      <table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0">
                        <tr>
                          <td width="7" style="width:7px; border-radius:99px; background:#CBFF4D;"></td>
                          <td style="padding-left:16px;">
                            <div style="color:#FFBF59; font-size:12px; line-height:16px; font-weight:900; letter-spacing:1.6px; text-transform:uppercase;">__EYEBROW__</div>
                            <h1 class="pf-title" style="margin:8px 0 10px 0; font-size:36px; line-height:42px; font-weight:900; letter-spacing:0; color:#FFFFFF;">__TITLE__</h1>
                          </td>
                        </tr>
                      </table>
                      <div style="margin-top:20px; font-size:16px; line-height:27px; color:#DADAEA;">__BODY__</div>
                      __ACTION__
                      __AFTER__
                      <table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0" style="margin-top:30px; border-top:1px solid #2A2A35;">
                        <tr>
                          <td style="padding-top:18px; color:#A8A8BA; font-size:13px; line-height:21px;">
                            Bu işlemi sen başlatmadıysan bu maili yok sayabilirsin. Hesabın güvende kalmaya devam eder.
                          </td>
                        </tr>
                      </table>
                    </td>
                  </tr>
                </table>
              </td>
            </tr>
            <tr>
              <td align="center" style="padding:22px 18px 0 18px; color:#67677F; font-size:12px; line-height:20px;">
                © 2026 Profitness · Avonix<br>
                Bu otomatik bir mesajdır, lütfen yanıtlamayın.
              </td>
            </tr>
          </table>
        </td>
      </tr>
    </table>
  </body>
</html>
'@

    return $template.
        Replace("__PREHEADER__", $Preheader).
        Replace("__LOGO_URL__", $LogoUrl).
        Replace("__EYEBROW__", $Eyebrow).
        Replace("__TITLE__", $Title).
        Replace("__BODY__", $BodyHtml).
        Replace("__ACTION__", $ActionHtml).
        Replace("__AFTER__", $AfterHtml)
}

function New-CodeBlock {
    param([string] $Token = "{{ .Token }}")

    return @"
<table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0" style="margin:30px 0 10px 0; background:#07070B; border:1px solid #2A2A35; border-radius:24px; overflow:hidden;">
  <tr>
    <td height="5" style="height:5px; background:#CBFF4D; background-image:linear-gradient(90deg,#CBFF4D,#00E5D3,#FF6B6B,#FFBF59); font-size:0; line-height:0;">&nbsp;</td>
  </tr>
  <tr>
    <td align="center" style="padding:34px 16px 30px 16px;">
      <div style="margin-bottom:14px; color:#FFBF59; font-size:11px; line-height:14px; font-weight:900; letter-spacing:1.8px;">DOĞRULAMA KODU</div>
      <div class="pf-code" style="font-family:'SFMono-Regular', Consolas, 'Liberation Mono', monospace; font-size:48px; line-height:54px; letter-spacing:14px; color:#CBFF4D; font-weight:900; white-space:nowrap; text-shadow:0 0 20px rgba(203,255,77,.24);">$Token</div>
      <div style="margin-top:16px; color:#B9B9CB; font-size:13px; line-height:19px;">Profitness uygulamasındaki kod ekranına gir</div>
    </td>
  </tr>
</table>
"@
}

function New-Button {
    param(
        [string] $Href,
        [string] $Label
    )

    return @"
<table role="presentation" cellspacing="0" cellpadding="0" border="0" style="margin-top:30px;">
  <tr>
    <td style="border-radius:18px; background:#CBFF4D; box-shadow:0 14px 30px rgba(203,255,77,.18);">
      <a href="$Href" style="display:inline-block; padding:17px 24px; color:#0A0A0F; font-size:15px; line-height:18px; font-weight:900; text-decoration:none; border-radius:18px;">$Label</a>
    </td>
  </tr>
</table>
"@
}

$templates = @{
    confirmation = @{
        Subject = "Profitness doğrulama kodun"
        Html = New-ProfitnessEmail `
            -Preheader "Profitness hesabını doğrulamak için 6 haneli kodun hazır." `
            -Eyebrow "Kayıt doğrulama" `
            -Title "Hesabını doğrula" `
            -BodyHtml "Profitness'e hoş geldin. Uygulamaya dön ve aşağıdaki 6 haneli kodu girerek email adresini doğrula." `
            -ActionHtml (New-CodeBlock) `
            -AfterHtml '<p style="margin:18px 0 0 0; color:#9A9AB0; font-size:14px; line-height:22px;">Kod kısa süre geçerlidir. Süresi dolarsa uygulamadan yeni kod isteyebilirsin.</p>'
    }
    recovery = @{
        Subject = "Profitness şifre yenileme bağlantısı"
        Html = New-ProfitnessEmail `
            -Preheader "Profitness hesabın için şifre yenileme bağlantın hazır." `
            -Eyebrow "Şifre yenileme" `
            -Title "Şifreni yenile" `
            -BodyHtml "Profitness hesabın için şifre yenileme isteği aldık. Devam etmek için aşağıdaki güvenli bağlantıyı kullan." `
            -ActionHtml (New-Button -Href "{{ .ConfirmationURL }}" -Label "Şifreyi yenile") `
            -AfterHtml '<p style="margin:18px 0 0 0; color:#9A9AB0; font-size:14px; line-height:22px;">Bağlantı çalışmazsa tarayıcına şu adresi yapıştır:<br><span style="color:#C7C7D6; word-break:break-all;">{{ .ConfirmationURL }}</span></p>'
    }
    magic_link = @{
        Subject = "Profitness giriş bağlantın"
        Html = New-ProfitnessEmail `
            -Preheader "Profitness'e tek dokunuşla giriş yapmak için bağlantın hazır." `
            -Eyebrow "Güvenli giriş" `
            -Title "Profitness'e dön" `
            -BodyHtml "Bu bağlantı seni Profitness hesabına güvenli şekilde yönlendirir." `
            -ActionHtml (New-Button -Href "{{ .ConfirmationURL }}" -Label "Giriş yap") `
            -AfterHtml '<p style="margin:18px 0 0 0; color:#9A9AB0; font-size:14px; line-height:22px;">Bağlantı çalışmazsa tarayıcına şu adresi yapıştır:<br><span style="color:#C7C7D6; word-break:break-all;">{{ .ConfirmationURL }}</span></p>'
    }
    invite = @{
        Subject = "Profitness davetin var"
        Html = New-ProfitnessEmail `
            -Preheader "Profitness'e katılman için bir davet aldın." `
            -Eyebrow "Davet" `
            -Title "Profitness'e katıl" `
            -BodyHtml "Sana Profitness'te hesap oluşturman için davet gönderildi. Daveti kabul ederek antrenman yolculuğuna başlayabilirsin." `
            -ActionHtml (New-Button -Href "{{ .ConfirmationURL }}" -Label "Daveti kabul et") `
            -AfterHtml '<p style="margin:18px 0 0 0; color:#9A9AB0; font-size:14px; line-height:22px;">Bağlantı çalışmazsa tarayıcına şu adresi yapıştır:<br><span style="color:#C7C7D6; word-break:break-all;">{{ .ConfirmationURL }}</span></p>'
    }
    email_change = @{
        Subject = "Profitness email değişikliği onayı"
        Html = New-ProfitnessEmail `
            -Preheader "Profitness hesabındaki email değişikliğini onayla." `
            -Eyebrow "Email değişikliği" `
            -Title "Yeni email adresini onayla" `
            -BodyHtml 'Profitness hesabının email adresini <strong style="color:#F8F8F8;">{{ .NewEmail }}</strong> olarak güncellemek için aşağıdaki güvenli bağlantıyı kullan.' `
            -ActionHtml (New-Button -Href "{{ .ConfirmationURL }}" -Label "Email'i onayla") `
            -AfterHtml '<p style="margin:18px 0 0 0; color:#9A9AB0; font-size:14px; line-height:22px;">Bağlantı çalışmazsa tarayıcına şu adresi yapıştır:<br><span style="color:#C7C7D6; word-break:break-all;">{{ .ConfirmationURL }}</span></p>'
    }
    reauthentication = @{
        Subject = "Profitness güvenlik kodun"
        Html = New-ProfitnessEmail `
            -Preheader "Hassas işlem için Profitness güvenlik kodun hazır." `
            -Eyebrow "Güvenlik kontrolü" `
            -Title "İşlemi onayla" `
            -BodyHtml "Hesabındaki hassas işlemi tamamlamak için aşağıdaki kodu Profitness uygulamasına gir." `
            -ActionHtml (New-CodeBlock) `
            -AfterHtml '<p style="margin:18px 0 0 0; color:#9A9AB0; font-size:14px; line-height:22px;">Kod kısa süre geçerlidir. Bu isteği sen başlatmadıysan herhangi bir işlem yapmana gerek yok.</p>'
    }
}

New-Item -ItemType Directory -Force -Path $TemplateDir | Out-Null

foreach ($name in $templates.Keys) {
    $path = Join-Path $TemplateDir "$name.html"
    Set-Content -Path $path -Value $templates[$name].Html -Encoding UTF8
}

if ($WriteOnly) {
    Write-Host "Templates written to $TemplateDir"
    return
}

if ([string]::IsNullOrWhiteSpace($env:SUPABASE_ACCESS_TOKEN)) {
    throw "SUPABASE_ACCESS_TOKEN ortam değişkeni gerekli."
}

$payload = @{
    mailer_subjects_confirmation = $templates.confirmation.Subject
    mailer_templates_confirmation_content = $templates.confirmation.Html
    mailer_subjects_recovery = $templates.recovery.Subject
    mailer_templates_recovery_content = $templates.recovery.Html
    mailer_subjects_magic_link = $templates.magic_link.Subject
    mailer_templates_magic_link_content = $templates.magic_link.Html
    mailer_subjects_invite = $templates.invite.Subject
    mailer_templates_invite_content = $templates.invite.Html
    mailer_subjects_email_change = $templates.email_change.Subject
    mailer_templates_email_change_content = $templates.email_change.Html
    mailer_subjects_reauthentication = $templates.reauthentication.Subject
    mailer_templates_reauthentication_content = $templates.reauthentication.Html
}

$json = $payload | ConvertTo-Json -Depth 10
$uri = "https://api.supabase.com/v1/projects/$ProjectRef/config/auth"

Invoke-RestMethod `
    -Method Patch `
    -Uri $uri `
    -Headers @{ Authorization = "Bearer $env:SUPABASE_ACCESS_TOKEN" } `
    -ContentType "application/json" `
    -Body $json | Out-Null

Write-Host "Profitness Auth email templates updated for project $ProjectRef"
