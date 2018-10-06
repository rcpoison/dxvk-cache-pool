# Getting verified

For the moment I will verify your keys manually until I've implemented automation for it.

Required from you:
- Your **public** GPG key in binary format (`gpg --output public.key --export your-key-id`)
- Your **public** dxvk-cache-pool  key (`$XDG_CONFIG_HOME/dxvk-cache-pool/ec.pub`)
- A *detached* GPG signature for your dxvk-cache-pool **public** key (`gpg --output ec.pub.sig --detach-sig ec.pub`)

And create a tarball/zip it.

(Or you can just use the [requestVerification.sh](requestVerification.sh) script.)

Either create a ticket and attach your verification information or send it to me by email (rc dot poison at gmail dot com).
