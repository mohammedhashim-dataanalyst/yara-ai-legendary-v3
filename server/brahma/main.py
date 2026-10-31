"""Reference-only FastAPI boundary for a future authenticated Brahma gateway.
The Android APK does not import or require this module.
"""
from fastapi import FastAPI
from pydantic import BaseModel

app = FastAPI(title="Brahma Reference Gateway")

class Health(BaseModel):
    service: str = "brahma-reference"
    native_android: bool = True

@app.get("/health", response_model=Health)
def health() -> Health:
    return Health()

# Future work: authenticated provider proxy, Home Assistant, Discord, and websocket PC gateway.
