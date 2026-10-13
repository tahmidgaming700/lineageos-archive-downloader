
set -e

mkdir -p "$HOME/.android-signing"
chmod 700 "$HOME/.android-signing"

KEYSTORE="$HOME/.android-signing/lineage-release.jks"

# Enter a strong password without displaying it.
read -rsp "Choose a strong signing password: " KS_PASS
echo
read -rsp "Enter it again: " KS_PASS2
echo

if [ "$KS_PASS" != "$KS_PASS2" ] || [ "${#KS_PASS}" -lt 12 ]; then
  unset KS_PASS KS_PASS2
  echo "Passwords must match and be at least 12 characters."
  exit 1
fi
unset KS_PASS2

# Generate a new signing key.
keytool -genkeypair -noprompt \
  -keystore "$KEYSTORE" \
  -storetype JKS \
  -storepass "$KS_PASS" \
  -keypass "$KS_PASS" \
  -alias lineage-updater \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -dname "CN=Lineage Updater, O=Personal, L=Dhaka, ST=Dhaka, C=BD"

chmod 600 "$KEYSTORE"

# Extract the certificate fingerprint.
CERT=$(keytool -list -v \
  -keystore "$KEYSTORE" \
  -alias lineage-updater \
  -storepass "$KS_PASS" |
  awk -F': ' '/SHA256:/{print $2; exit}')

if [ -z "$CERT" ]; then
  echo "Could not read the certificate fingerprint."
  unset KS_PASS
  exit 1
fi

# Upload secrets directly to the repository.
base64 -w 0 "$KEYSTORE" |
  gh secret set ANDROID_KEYSTORE_BASE64

printf '%s' "$KS_PASS" |
  gh secret set ANDROID_KEYSTORE_PASSWORD

printf '%s' "$KS_PASS" |
  gh secret set ANDROID_KEY_PASSWORD

printf '%s' "lineage-updater" |
  gh secret set ANDROID_KEY_ALIAS

printf '%s' "$CERT" |
  gh secret set ANDROID_CERT_SHA256

unset KS_PASS CERT

echo "Signing secrets uploaded."
gh secret list
