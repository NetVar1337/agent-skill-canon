# Libvirt domain shape for qemu-anti-detection

Source: zhaodice README. Replace UUID. Do not copy serials from a public gist onto a long-lived lab box.

```xml
<domain xmlns:qemu="http://libvirt.org/schemas/domain/qemu/1.0" type="kvm">
  <name>Entertainment</name>
  <uuid>REPLACE-YOUR-UUID</uuid>
  <memory unit="KiB">1548288</memory>
  <currentMemory unit="KiB">1548288</currentMemory>
  <memoryBacking>
    <source type="memfd"/>
    <access mode="shared"/>
  </memoryBacking>
  <vcpu placement="static">12</vcpu>
  <os firmware="efi">
    <type arch="x86_64" machine="pc-q35-7.0">hvm</type>
    <loader/>
    <smbios mode="host"/>
  </os>
  <features>
    <acpi/>
    <apic/>
    <hyperv mode="custom">
      <relaxed state="on"/>
      <vapic state="on"/>
      <spinlocks state="on" retries="8191"/>
      <vendor_id state="on" value="GenuineIntel"/>
    </hyperv>
    <kvm>
      <hidden state="on"/>
    </kvm>
    <vmport state="off"/>
    <smm state="on"/>
    <ioapic driver="kvm"/>
  </features>
  <cpu mode="host-passthrough" check="none" migratable="on">
    <feature policy="disable" name="hypervisor"/>
  </cpu>
  <clock offset="localtime">
    <timer name="rtc" tickpolicy="catchup"/>
    <timer name="pit" tickpolicy="delay"/>
    <timer name="hpet" present="no"/>
    <timer name="hypervclock" present="yes"/>
  </clock>
  <qemu:commandline>
    <qemu:arg value="-smbios"/>
    <qemu:arg value="type=0,version=UX305UA.201"/>
    <qemu:arg value="-smbios"/>
    <qemu:arg value="type=1,manufacturer=ASUS,product=UX305UA,version=2021.1"/>
    <qemu:arg value="-smbios"/>
    <qemu:arg value="type=2,manufacturer=Intel,version=2021.5,product=Intel i9-12900K"/>
    <qemu:arg value="-smbios"/>
    <qemu:arg value="type=3,manufacturer=XBZJ"/>
    <qemu:arg value="-smbios"/>
    <qemu:arg value="type=17,manufacturer=KINGSTON,loc_pfx=DDR5,speed=4800,serial=000000,part=0000"/>
    <qemu:arg value="-smbios"/>
    <qemu:arg value="type=4,manufacturer=Intel,max-speed=4800,current-speed=4800"/>
    <qemu:arg value="-cpu"/>
    <qemu:arg value="host,family=6,model=158,stepping=2,model_id=Intel(R) Core(TM) i9-12900K CPU @ 2.60GHz,vmware-cpuid-freq=false,enforce=false,host-phys-bits=true,hypervisor=off"/>
    <qemu:arg value="-machine"/>
    <qemu:arg value="q35,kernel_irqchip=on"/>
  </qemu:commandline>
</domain>
```

## What the patch itself rewrites

Typical delta vs stock QEMU (confirm in the versioned `.patch`):

- USB/HID product strings (`QEMU keyboard` → OEM-like)
- Disk / NIC / audio model names and serials
- SMBIOS type 0/1 defaults
- UEFI VM bit
- Boot Graphics Resource Table vendor

XML and the QEMU patch are not substitutes. A stock binary with this XML still leaks QEMU in device IDs. A patched binary with a default `pc-i440fx` + `qemu64` CPU still leaks hypervisor CPUID.

## Guest string hunt after boot

```
wmic bios get manufacturer,smbiosbiosversion,serialnumber
wmic computersystem get manufacturer,model
wmic baseboard get manufacturer,product,serialnumber
wmic diskdrive get model,serialnumber
wmic path Win32_Keyboard get *
wmic path Win32_Fan get *
```

Any `QEMU`, `BOCHS`, `SeaBIOS`, `Red Hat`, `VirtIO`, `0000` cloned serial is a finding. Fan/sensor WMI returning empty is a known leak, not a pass.
