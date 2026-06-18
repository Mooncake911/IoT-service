cd infra/terraform

yc iam key create --service-account-name github-actions-sa --output sa-key.json
yc iam key list --service-account-name github-actions-sa   

yc iam access-key create --service-account-name github-actions-sa --format json | Out-File -FilePath sa-access-key.json -Encoding utf8    
yc iam access-key list --service-account-name github-actions-sa

$json = Get-Content sa-access-key.json | ConvertFrom-Json
D:\Projects\IDEA_Projects\IoT-service\terraform_1.15.5_windows_amd64\terraform.exe init `
    -backend-config="access_key=$($json.access_key.key_id)" `
    -backend-config="secret_key=$($json.secret)" `
    -reconfigure 

D:\Projects\IDEA_Projects\IoT-service\terraform_1.15.5_windows_amd64\terraform.exe state list

D:\Projects\IDEA_Projects\IoT-service\terraform_1.15.5_windows_amd64\terraform.exe plan

D:\Projects\IDEA_Projects\IoT-service\terraform_1.15.5_windows_amd64\terraform.exe apply

D:\Projects\IDEA_Projects\IoT-service\terraform_1.15.5_windows_amd64\terraform.exe destroy

yc kms symmetric-key list  
D:\Projects\IDEA_Projects\IoT-service\terraform_1.15.5_windows_amd64\terraform.exe state rm yandex_kms_symmetric_key.backup_key                                