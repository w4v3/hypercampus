package onion.w4v3xrmknycexlsd.app.hypercampus

import android.app.Application

class HyperApp: Application() { val hyperComponent = DaggerHyperComponent.builder().application(this).build() }