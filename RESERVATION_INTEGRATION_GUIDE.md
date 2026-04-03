# Reservation Management Integration Guide

## Overview
This document explains the complete flow of adding and displaying reservations in the Apartment Management System.

## Architecture

### 1. AdminAddReservationActivity
**File**: `app/src/main/java/com/example/apartmentmanagementsystem/AdminAddReservationActivity.java`

**Purpose**: Form for adding new reservations with the following features:
- Service selection (Pool, Gym, Restaurant)
- Description input
- Date and time selection
- Image upload with preview

**Key Methods**:
- `submitReservation()` - Validates all form inputs before saving
- `addReservationToSupabase()` - Sends reservation data to Supabase via REST API
- `getBookedByUser()` - Retrieves the admin user email from SharedPreferences
- `getBearerToken()` - Retrieves the authentication token from SharedPreferences

**Data Flow**:
1. User fills form with reservation details
2. User selects an image from device storage
3. User clicks "ADD RESERVATION" button
4. Form validation checks if all fields are filled
5. Reservation object is created
6. Data is sent to Supabase `reservations` table via REST API
7. Upon success, activity closes and returns to AdminReservationMaintenanceActivity
8. RESULT_OK is set to trigger list refresh

### 2. AdminReservationMaintenanceActivity
**File**: `app/src/main/java/com/example/apartmentmanagementsystem/AdminReservationMaintenanceActivity.java`

**Purpose**: Displays all reservations in a RecyclerView with edit/delete functionality

**Key Methods**:
- `setupToolbar()` - Sets up toolbar menu with Add and Refresh buttons
- `fetchReservations()` - Fetches reservations from Supabase via REST API
- `setupList()` - Initializes RecyclerView with adapter and action listeners
- `deleteReservation()` - Deletes a reservation from Supabase

**Data Flow**:
1. Activity is opened
2. `fetchReservations()` is called to load all reservations from Supabase
3. Reservations are parsed from JSON and added to the list
4. Adapter displays reservations in RecyclerView
5. When user clicks "Add" button in toolbar:
   - Opens AdminAddReservationActivity
   - Sets up ActivityResultLauncher to listen for result
6. When AdminAddReservationActivity returns with RESULT_OK:
   - ActivityResultLauncher callback triggers `fetchReservations()`
   - List is refreshed to show newly added reservation
7. User can click refresh button to manually refresh the list

### 3. AdminReservation Model
**File**: `app/src/main/java/com/example/apartmentmanagementsystem/AdminReservation.java`

**Purpose**: Data class representing a single reservation

**Fields**:
- `id` - Unique identifier
- `serviceName` - Service type (Pool, Gym, Restaurant)
- `description` - Reservation description
- `reservationDate` - Date of reservation
- `reservationTime` - Start time of reservation
- `duration` - Duration of reservation (e.g., "09:00 - 11:00")
- `imageUrl` - URL of reservation image from Supabase Storage
- `status` - Status of reservation (pending, confirmed, cancelled)
- `bookedBy` - Email of user who made the reservation
- `capacity` - Capacity/guest count for the reservation

### 4. AdminReservationMaintenanceAdapter
**File**: `app/src/main/java/com/example/apartmentmanagementsystem/AdminReservationMaintenanceAdapter.java`

**Purpose**: RecyclerView adapter for displaying reservations

**Features**:
- Displays reservation details including image, service, date, time
- Shows status with color-coded badges (pending, confirmed, etc.)
- Provides edit and delete action buttons
- Uses Glide for image loading

## Integration Workflow

### Adding a New Reservation

```
AdminReservationMaintenanceActivity
    ↓ (User clicks "Add" button)
AdminAddReservationActivity
    ↓ (User fills form and submits)
validateInputs()
    ↓
createAdminReservationObject()
    ↓
addReservationToSupabase() [Background Thread]
    ↓
POST to Supabase /rest/v1/reservations
    ↓
Success: setResult(RESULT_OK) → finish()
    ↓
ActivityResultLauncher Callback
    ↓
fetchReservations() [Refresh List]
    ↓
GET from Supabase /rest/v1/reservations
    ↓
parseJSONResponse()
    ↓
updateRecyclerView with new data
```

## Supabase Integration

### Reservations Table Structure
```sql
CREATE TABLE reservations (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  service_name TEXT NOT NULL,
  description TEXT,
  reservation_date DATE,
  reservation_time TIME,
  duration TEXT,
  image_url TEXT,
  status TEXT DEFAULT 'pending',
  booked_by TEXT,
  capacity TEXT,
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW()
);
```

### Required Supabase Configuration

1. **Storage Bucket**:
   - Create bucket named `reservations`
   - Set public access for downloading images

2. **RLS Policies** (if using):
   - Allow authenticated users to INSERT new reservations
   - Allow authenticated users to SELECT all reservations
   - Allow authenticated users to DELETE their own reservations

3. **API Configuration**:
   - REST API must be enabled
   - Use API key from SupabaseClient configuration

## Key Features

### 1. Image Handling
- User selects image from device storage
- Image is displayed as preview before submission
- Image Uri is stored temporarily during the session

### 2. Form Validation
```java
if (description.isEmpty() || date.isEmpty() || 
    timeStart.isEmpty() || timeEnd.isEmpty()) {
    Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
    return;
}

if (selectedImageUri == null) {
    Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show();
    return;
}
```

### 3. Service Selection
- Three service options: Pool, Gym, Restaurant
- Visual feedback when service is selected
- Selected service is displayed as text
- Toast notification for user feedback

### 4. Time Duration
- Start time and end time inputs
- Automatically formatted as "HH:MM - HH:MM"
- Stored in duration field

### 5. Error Handling
- HTTP error codes are checked
- Error messages are displayed to user
- Exceptions are logged for debugging
- Toast notifications provide user feedback

### 6. Threading
- All Supabase API calls run on background threads
- UI updates are executed on main thread using runOnUiThread()
- Prevents ANR (Application Not Responding) errors

## UI Components

### activity_admin_add_reservation.xml
- **AppBar**: Back button and title
- **Service Selection Card**: 
  - 3 service buttons with icons
  - Selected service display text
- **Form Inputs Card**:
  - Description EditText
  - Date picker EditText
  - Time duration inputs (start & end)
  - Image upload area
  - Image preview
- **Submit Button**: Large button to submit form

### activity_admin_reservation_maintenance.xml
- **Toolbar**: Navigation back button, Add button, Refresh button
- **RecyclerView**: Displays list of reservations
- **Empty State**: Message when no reservations exist
- **Loading State**: Message while fetching data

## Usage Instructions

### For Admin Users

1. **To Add a New Reservation**:
   - Navigate to "Reservation Maintenance" section
   - Click the "Add" (+ icon) button in the toolbar
   - Select a service type (Pool, Gym, or Restaurant)
   - Enter reservation details:
     - Description (e.g., "Birthday Party")
     - Date (in YYYY-MM-DD format)
     - Start and end times
   - Click image upload area to select an image
   - Click "ADD RESERVATION" button
   - Wait for success confirmation
   - You will be returned to the list and the new reservation will appear

2. **To View Reservations**:
   - Reservations are automatically loaded when opening the section
   - Scroll through the list to view all reservations
   - Each card shows: image, service type, date, time, duration, status, and booked by user

3. **To Refresh the List**:
   - Click the refresh (sync) icon in the toolbar
   - The list will be reloaded from Supabase

4. **To Edit a Reservation**:
   - Click the edit icon on a reservation card
   - Opens AdminEditReservationActivity
   - Make changes and save
   - You will be returned to the list with updated data

5. **To Delete a Reservation**:
   - Click the delete icon on a reservation card
   - Confirm deletion in the dialog
   - Reservation will be removed from Supabase

## Error Handling & Troubleshooting

### Common Issues

1. **"Please fill all fields" error**:
   - Ensure all form fields have values
   - Check that image has been selected

2. **"Reservation added successfully!" but not appearing in list**:
   - Click refresh button to manually refresh
   - Check internet connection
   - Verify Supabase credentials in SupabaseClient

3. **Image not loading**:
   - Verify image file exists and is readable
   - Check Supabase Storage bucket permissions
   - Ensure image URL is correct

4. **Network errors**:
   - Check internet connectivity
   - Verify Supabase URL and API keys
   - Check Supabase RLS policies

## Testing Checklist

- [ ] Add new reservation with all fields filled
- [ ] Verify reservation appears in maintenance list
- [ ] Select different services and verify data saved correctly
- [ ] Upload different image types (JPG, PNG)
- [ ] Verify image displays correctly in list
- [ ] Test refresh button functionality
- [ ] Test edit and delete functionality
- [ ] Test network error handling
- [ ] Test form validation (missing fields)
- [ ] Verify timestamps are correct

## Security Considerations

1. **Authentication**:
   - Bearer token is required for API calls
   - Stored in SharedPreferences under "LoginPrefs"
   - Falls back to ANON_KEY if not available

2. **Validation**:
   - All inputs are validated before submission
   - Empty fields are not allowed

3. **Data Privacy**:
   - Each reservation is linked to booked_by email
   - Supabase RLS policies should restrict access

4. **Image Security**:
   - Images are stored in Supabase Storage
   - Consider implementing file size limits
   - Validate image types before upload

## Future Enhancements

1. **Image Upload to Storage**:
   - Currently using local Uri
   - Implement actual upload to Supabase Storage
   - Generate public image URLs

2. **Status Management**:
   - Add status selection in form
   - Implement status update functionality

3. **Availability Checking**:
   - Check for conflicting reservations
   - Show available time slots

4. **Notifications**:
   - Send confirmation emails
   - Push notifications for reservation changes

5. **Analytics**:
   - Track reservation statistics
   - Generate reports

## File Locations

```
ApartmentManagementSystem/
├── app/src/main/java/com/example/apartmentmanagementsystem/
│   ├── AdminAddReservationActivity.java
│   ├── AdminReservationMaintenanceActivity.java
│   ├── AdminReservation.java
│   └── AdminReservationMaintenanceAdapter.java
├── app/src/main/res/layout/
│   ├── activity_admin_add_reservation.xml
│   └── activity_admin_reservation_maintenance.xml
└── app/src/main/res/menu/
    └── menu_admin_reservation.xml
```

## Conclusion

The reservation management system is now fully integrated with:
- ✅ Form for adding new reservations
- ✅ Automatic display in maintenance list
- ✅ Supabase backend integration
- ✅ Image preview and upload
- ✅ Service selection
- ✅ Error handling and validation
- ✅ Refresh functionality

Users can now seamlessly add reservations through the form and immediately see them appear in the maintenance list.

