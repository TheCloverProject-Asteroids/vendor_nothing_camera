# Nothing Camera for Nothing Phone (3a)

Nothing Camera (`com.nothing.camera`) for the Nothing Phone (3a) / (3a) Pro
(asteroids) on AOSP-based ROMs, as a drop-in extract-utils vendor repository.
Clone to `vendor/nothing/camera` and run `git lfs pull`.

## Integration

```make
# BoardConfig.mk
-include vendor/nothing/camera/BoardConfigCamera.mk

# device.mk
$(call inherit-product-if-exists, vendor/nothing/camera/nothing-camera.mk)
```

Both are optional includes, so the device still builds without this repo
(Aperture keeps working, Nothing Camera is simply absent).

Required in the device tree:

- `vendor/nothing/asteroids` must ship Nothing's
  `vendor.qti.hardware.camera.postproc@1.0` interface library as
  `vendor.qti.hardware.camera.postproc@1.0-nothing.so`, with
  `libntofflinepostproc.so` and `vendor.qti.hardware.camera.postproc@1.0-service-impl.so`
  relinked to it. Nothing extended `IPostProcSession` with `release()` under the
  same package version, so the AOSP/QTI interface library breaks the in-HAL
  post-processing path (`camerapostprocservice`) for every capture.
- `vendor/lib64/libcameradecision.so` listed for extraction when building from
  firmware whose `public.libraries.txt` names it.

## Contents

| Path | |
|---|---|
| `NTCamera.apk` | stored with Git LFS (`git lfs pull` after cloning). Built from `app/Android.bp` (symlinked APK) so it is dexpreopted like stock. See `patches/NTCamera.md` for its bytecode changes. |
| `libofflineproc_jni` | NTCamera's JNI for `media.offline` |
| `offlineproc/` | `libofflineproc_shim`: `IOfflineProcService`/`IOfflineProcClient` (AIDL reconstructed from Nothing's `libcamera_client`, same transaction codes and vtables) plus `OfflineParameters` and `OfflineProcClient` with Nothing's object layout. `ntcam_offlineproc`: lazy `media.offline` service. |
| `libntcam_client` | system_ext client library `libofflineproc_jni` links against |
| `shims/libntcam_session_shim` | prebuilt, see below |
| `vendor.noth.hardware.camera-service` | Nothing's algorithm HAL (`INtCamService`), run in `hal_camera_default` (QTI camera provider domain) |
| `sepolicy/` | Mirrors the stock policy for the above, plus the `platform_app`/`platform_app_36` client rules |

### Why `media.offline` exists

The JNI polls `getService("media.offline")` in an endless 500 ms loop until the
service appears. Nothing OS publishes it from cameraserver and backs it with
`/vendor/lib64/libofflinepostprocclient.so`, which Phone (3a) firmware does not
ship, so stock answers `initOffline()` with `-EINVAL` and NTCamera processes in
the camera HAL. `ntcam_offlineproc` answers the same way, and starts only while
the camera holds it.

## Updating

```sh
./extract-files.py <path to Nothing OS dump>   # regenerates makefiles too
```

`NTCamera.apk`, `libofflineproc_jni`, `libntcam_client` and
`libQnnHtpV73Stub` are not in stock firmware and are pinned by SHA1 in
`proprietary-files.txt`, so extract-utils keeps the copies in this repo.
Drop the pin and re-apply `patches/NTCamera.md` to move to a new APK.

## Device-tree side (not in this repo)

Add these to the device's `extract-files.py` so they survive re-extraction:

```python
lib_fixups = {
    ...
    ('libntcam_session_shim',): lib_fixup_remove,  # module lives here, keep the device buildable without it
}

blob_fixups = {
    # Load libntcam_session_shim first: it wraps hw_get_module() and tags
    # sessions from com.nothing.camera with package_name/activity_name so
    # CamX selects the Nothing pipeline.
    'vendor/bin/hw/vendor.qti.camera.provider-service_64': blob_fixup()
        .add_needed('libntcam_session_shim.so'),
    # Portrait: cbz x23 at 0xc7d8 retargeted to 0xc6ac so the node falls back
    # to the /data/vendor/camera calibration files.
    'vendor/lib64/camera/node/com.nothing.node.bokehhdr.so': blob_fixup()
        .binary_regex_replace(b'\xb7\x83\x5d\xf8\x77\xf1\xff\xb4', b'\xb7\x83\x5d\xf8\xb7\xf6\xff\xb4'),
}
```

and mark the provider `;DISABLE_CHECKELF` in `proprietary-files.txt`, since
its new `DT_NEEDED` is not visible from the device's soong namespace.

The HAL needs `hal_camera_default` to keep its stock QTI permissions (DSP,
`vendor_camera_prop`, postproc hwservice); no changes to
`device/qcom/sepolicy_vndr` are needed.

After flashing, run `adb shell pm clear com.nothing.camera` once to drop
stale capture queue entries left by earlier builds.

## Credits

- Omkar Chandorkar (Paranoid Android) - repository layout, extract-utils
  camera module and the libcamera_client shim approach, from
  [ThankYouMario/proprietary_vendor_nothing_camera@b61fb25c76](https://github.com/ThankYouMario/proprietary_vendor_nothing_camera/commit/b61fb25c762a8101420f243772b089e1b6d6c0aa)
- Aniket (crdroid-asteroids) - working Nothing Camera bring-up on
  asteroids used as a cross-reference for the post-processing interface
  relink and provider integration:
  [819622a22f](https://github.com/crdroid-asteroids/device_nothing_asteroids/commit/819622a22f79933503b60b2fb365e4b9e0173ce4),
  [1922fc8530](https://github.com/crdroid-asteroids/device_nothing_asteroids/commit/1922fc85301d5b6820aeb2a60c4ab22dc59a2e0b)
- Fuzail Mansuri - original NT-cam port and the NTCamera bytecode
  patches,
  [9982113fcf](https://github.com/fuzailmansuri/NT-cam/commit/9982113fcf4e60ba6d4c7c76e73f1f28ce8b5ac6)
- Stock Nothing OS firmware (permissions, SELinux policy, binaries used for
  reverse engineering) from
  [leddaz-dump-stash/nothing_asteroids_dump@6c10386180](https://github.com/leddaz-dump-stash/nothing_asteroids_dump/tree/6c10386180c83031c983f569c3c956ebfe8a9c27)
