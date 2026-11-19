package com.unboring.weather.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unboring.weather.data.LicenseManager
import com.unboring.weather.model.SkinCatalog
import com.unboring.weather.model.SkinId
import com.unboring.weather.model.WeatherLocation
import com.unboring.weather.model.WeatherSkin
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkinsAndProSheet(
  activeSkinId: SkinId,
  isPro: Boolean,
  onSelectSkin: (SkinId) -> Unit,
  onVerifyLicense: suspend (String) -> Result<String>,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var showLicenseBox by remember { mutableStateOf(false) }
  var keyInput by remember { mutableStateOf("") }
  var statusMsg by remember { mutableStateOf<String?>(null) }
  var busy by remember { mutableStateOf(false) }

  ModalBottomSheet(onDismissRequest = onDismiss) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text("3D SKINS", fontSize = 22.sp, fontWeight = FontWeight.Black)
          Text(
            if (isPro) "PRO UNLOCKED • ALL SKINS ACTIVE" else "FREE + PRO COLLECTIBLE MATERIALS",
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = Color.Gray
          )
        }
        if (!isPro) {
          Button(onClick = { showLicenseBox = !showLicenseBox }) {
            Text("UNLOCK PRO")
          }
        }
      }

      if (showLicenseBox && !isPro) {
        Spacer(modifier = Modifier.height(12.dp))
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1A1C22))
            .padding(14.dp)
        ) {
          Text(
            "Unlock all 3D Skins, Debug Shaders & Pro Features",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
          )
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = keyInput,
            onValueChange = { keyInput = it },
            label = { Text("Gumroad / License Key") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          Spacer(modifier = Modifier.height(8.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
              enabled = !busy,
              onClick = {
                busy = true
                scope.launch {
                  val res = onVerifyLicense(keyInput)
                  statusMsg = res.fold(onSuccess = { it }, onFailure = { it.message })
                  busy = false
                }
              }
            ) {
              Text(if (busy) "VERIFYING..." else "ACTIVATE")
            }
            OutlinedButton(
              onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(LicenseManager.CHECKOUT_URL))
                context.startActivity(intent)
              }
            ) {
              Text("GET KEY")
            }
          }
          TextButton(
            onClick = { keyInput = "UNBORING-PRO-DEMO" }
          ) {
            Text("Fill Demo Key (UNBORING-PRO-DEMO)", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
          }
          statusMsg?.let {
            Text(it, color = Color(0xFF69F0AE), fontSize = 12.sp)
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.height(340.dp)
      ) {
        items(SkinCatalog.all) { skin: WeatherSkin ->
          val locked = skin.id.isPro && !isPro
          val selected = skin.id == activeSkinId
          Column(
            modifier = Modifier
              .clip(RoundedCornerShape(18.dp))
              .background(skin.background)
              .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) skin.accent else skin.onSurface.copy(alpha = 0.2f),
                shape = RoundedCornerShape(18.dp)
              )
              .clickable {
                if (locked) {
                  showLicenseBox = true
                  statusMsg = "${skin.id.displayName} is a Pro skin. Activate a key to unlock!"
                } else {
                  onSelectSkin(skin.id)
                  onDismiss()
                }
              }
              .padding(14.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(skin.sunColor))
                Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(skin.cloudColor))
                Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(skin.rainColor))
              }
              if (locked) {
                Icon(Icons.Rounded.Lock, contentDescription = "Locked", tint = skin.onSurface, modifier = Modifier.size(16.dp))
              } else if (selected) {
                Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = skin.accent, modifier = Modifier.size(18.dp))
              }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(skin.id.displayName, color = skin.onSurface, fontWeight = FontWeight.Black, fontSize = 16.sp)
            Text(skin.id.sku, color = skin.onSurface.copy(alpha = 0.65f), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSearchSheet(
  presets: List<WeatherLocation>,
  onSearch: suspend (String) -> List<WeatherLocation>,
  onSelectLocation: (WeatherLocation) -> Unit,
  onUseCurrentLocation: () -> Unit,
  onDismiss: () -> Unit
) {
  val scope = rememberCoroutineScope()
  var query by remember { mutableStateOf("") }
  var results by remember { mutableStateOf(presets) }

  ModalBottomSheet(onDismissRequest = onDismiss) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("LOCATIONS", fontSize = 22.sp, fontWeight = FontWeight.Black)
        TextButton(onClick = { onUseCurrentLocation(); onDismiss() }) {
          Text("USE GPS", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        }
      }
      Spacer(modifier = Modifier.height(10.dp))
      OutlinedTextField(
        value = query,
        onValueChange = { q ->
          query = q
          scope.launch { results = onSearch(q) }
        },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        placeholder = { Text("Search city...") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
      )
      Spacer(modifier = Modifier.height(12.dp))
      LazyColumn(modifier = Modifier.height(280.dp)) {
        items(results) { loc ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .clickable {
                onSelectLocation(loc)
                onDismiss()
              }
              .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(loc.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
              if (loc.region.isNotBlank()) {
                Text(loc.region, fontSize = 12.sp, color = Color.Gray)
              }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("SELECT", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
          }
        }
      }
      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}
