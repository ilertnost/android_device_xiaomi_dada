#
# SPDX-License-Identifier: Apache-2.0
#

# Apply the source patches of this device, its MIUI Camera tree and its common
# tree on top of the synced projects
# (see device/xiaomi/sm8750-common/patches/apply-patches.sh).
bash "$(gettop)/device/xiaomi/sm8750-common/patches/apply-patches.sh" \
    device/xiaomi/dada/patches \
    device/xiaomi/dada-miuicamera/patches \
    device/xiaomi/sm8750-common/patches
